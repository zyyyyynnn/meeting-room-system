from __future__ import annotations

import json
import time
import urllib.error
import urllib.request
from pathlib import Path

from selenium import webdriver
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.common.by import By
from selenium.webdriver.edge.options import Options as EdgeOptions
from selenium.webdriver.chrome.options import Options as ChromeOptions
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait


REPO_ROOT = Path(__file__).resolve().parents[2]
ASSETS_DIR = REPO_ROOT / "docs" / "experiment" / "assets"
FRONTEND_BASE = "http://127.0.0.1:5173"
BACKEND_BASE = "http://127.0.0.1:8080"
WINDOW_SIZE = (1600, 1100)


def api_login(username: str, password: str) -> dict[str, object]:
    body = json.dumps({"username": username, "password": password}).encode("utf-8")
    request = urllib.request.Request(
        url=f"{BACKEND_BASE}/api/auth/login",
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(request, timeout=20) as response:
        payload = json.loads(response.read().decode("utf-8"))
    if payload.get("code") != 0:
        raise RuntimeError(f"登录失败：{payload}")
    return payload["data"]


def create_driver() -> webdriver.Remote:
    edge_path = Path(r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe")
    chrome_path = Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe")

    if edge_path.exists():
        options = EdgeOptions()
        options.use_chromium = True
        options.binary_location = str(edge_path)
        for arg in (
            "--headless=new",
            "--disable-gpu",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--force-device-scale-factor=1",
            "--hide-scrollbars",
        ):
            options.add_argument(arg)
        driver = webdriver.Edge(options=options)
    elif chrome_path.exists():
        options = ChromeOptions()
        options.binary_location = str(chrome_path)
        for arg in (
            "--headless=new",
            "--disable-gpu",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--force-device-scale-factor=1",
            "--hide-scrollbars",
        ):
            options.add_argument(arg)
        driver = webdriver.Chrome(options=options)
    else:
        raise RuntimeError("未找到可用的 Edge 或 Chrome 浏览器，无法自动截图。")

    driver.set_window_size(*WINDOW_SIZE)
    return driver


def wait_for_text(driver: webdriver.Remote, text: str, timeout: int = 20) -> None:
    WebDriverWait(driver, timeout).until(
        EC.presence_of_element_located((By.XPATH, f"//*[contains(normalize-space(), '{text}')]"))
    )


def wait_for_stable(driver: webdriver.Remote, text: str, timeout: int = 20, settle_seconds: float = 1.6) -> None:
    wait_for_text(driver, text, timeout=timeout)
    WebDriverWait(driver, timeout).until(lambda d: d.execute_script("return document.readyState") == "complete")
    time.sleep(settle_seconds)


def set_auth_state(driver: webdriver.Remote, auth_data: dict[str, object]) -> None:
    driver.get(f"{FRONTEND_BASE}/login?force=1")
    wait_for_stable(driver, "登录系统")
    auth_payload = {
        "token": auth_data["token"],
        "userId": auth_data["userId"],
        "username": auth_data["username"],
        "role": auth_data["role"],
    }
    driver.execute_script(
        "window.localStorage.setItem('mrs_auth', arguments[0]);",
        json.dumps(auth_payload, ensure_ascii=False),
    )


def clear_auth_state(driver: webdriver.Remote) -> None:
    driver.get(f"{FRONTEND_BASE}/login?force=1")
    wait_for_stable(driver, "登录系统")
    driver.execute_script("window.localStorage.removeItem('mrs_auth');")


def capture_route(driver: webdriver.Remote, route: str, wait_text: str, output_name: str) -> Path:
    target = f"{FRONTEND_BASE}{route}"
    driver.get(target)
    wait_for_stable(driver, wait_text)
    output_path = ASSETS_DIR / output_name
    driver.save_screenshot(str(output_path))
    return output_path


def ensure_services_available() -> None:
    for url in (f"{FRONTEND_BASE}/login", f"{BACKEND_BASE}/doc.html"):
        try:
            with urllib.request.urlopen(url, timeout=10):
                continue
        except urllib.error.URLError as exc:
            raise RuntimeError(f"服务未就绪：{url}，详情：{exc}") from exc


def main() -> None:
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    ensure_services_available()

    root_auth = api_login("root", "Root@123456")
    admin_auth = api_login("admin", "admin123")

    driver = create_driver()
    generated: list[Path] = []
    try:
        clear_auth_state(driver)
        generated.append(capture_route(driver, "/login", "登录系统", "prototype-01-login.png"))

        set_auth_state(driver, root_auth)
        generated.append(capture_route(driver, "/dashboard", "运营看板", "prototype-02-dashboard.png"))
        generated.append(capture_route(driver, "/calendar", "会议室预约日历", "prototype-03-calendar.png"))
        generated.append(capture_route(driver, "/rooms", "会议室管理", "prototype-04-rooms.png"))
        generated.append(capture_route(driver, "/admin/approvals", "预约审批", "prototype-06-approvals.png"))

        set_auth_state(driver, admin_auth)
        generated.append(capture_route(driver, "/mine", "我的预约", "prototype-05-mine.png"))
    except TimeoutException as exc:
        raise RuntimeError("页面元素等待超时，截图未完成。") from exc
    finally:
        driver.quit()

    print("已生成截图：")
    for path in generated:
        print(path)


if __name__ == "__main__":
    main()
