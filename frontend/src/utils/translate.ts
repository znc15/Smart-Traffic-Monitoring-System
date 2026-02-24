export const translateBackendDetailToZh = (detail: string): string => {
  const map: Record<string, string> = {
    "Sai thông tin đăng nhập": "账号或密码错误",
    "Username, email hoặc số điện thoại đã tồn tại!": "用户名/邮箱/手机号已存在！",

    "Tên đăng nhập đã tồn tại!": "用户名已存在！",
    "Email đã được sử dụng!": "邮箱已被使用！",
    "Số điện thoại đã được sử dụng!": "手机号已被使用！",
    "Mật khẩu hiện tại không đúng!": "当前密码不正确！",

    "Chỉ admin mới được phép truy cập tài nguyên hệ thống.": "仅管理员可访问系统资源。",

    "Not authenticated": "未登录",
    Forbidden: "无权限",
    "Internal server error": "服务器内部错误",
  };

  return map[detail] ?? detail;
};
