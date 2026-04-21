from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    
    # Enable console log capture
    page.on("console", lambda msg: print(f"Browser console [{msg.type}]: {msg.text}"))
    page.on("pageerror", lambda err: print(f"Browser error: {err}"))
    
    print("Navigating to login...")
    page.goto('http://localhost:5173/login')
    page.wait_for_load_state('networkidle')
    
    print("Logging in...")
    page.fill('input[type="text"]', 'admin')
    page.fill('input[type="password"]', 'admin123')
    page.click('button[type="submit"]')
    
    page.wait_for_url('**/dashboard')
    print("Logged in successfully.")
    
    print("Navigating to Developer Center...")
    page.goto('http://localhost:5173/developer')
    page.wait_for_load_state('networkidle')
    
    print("Taking screenshot...")
    page.screenshot(path='/tmp/developer_center.png', full_page=True)
    
    print("Waiting a bit to let API calls finish...")
    page.wait_for_timeout(2000)
    
    print("Taking another screenshot after wait...")
    page.screenshot(path='/tmp/developer_center_after_wait.png', full_page=True)
    
    browser.close()