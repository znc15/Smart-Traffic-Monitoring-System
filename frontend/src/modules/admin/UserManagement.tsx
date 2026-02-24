import { useEffect, useState } from "react";
import { Button } from "@/ui/button";
import { adminConfig, authFetch } from "@/config";

type User = {
  id: number;
  username: string;
  email: string;
  phoneNumber: string;
  roleId: number;
  enabled: boolean;
  createdAt: string;
};

export default function UserManagement() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchUsers = async () => {
    try {
      const res = await authFetch(adminConfig.USERS_URL);
      if (res.ok) setUsers(await res.json());
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchUsers(); }, []);

  const toggleRole = async (u: User) => {
    await authFetch(`${adminConfig.USERS_URL}/${u.id}/role`, {
      method: "PUT",
      body: JSON.stringify({ roleId: u.roleId === 0 ? 1 : 0 }),
    });
    fetchUsers();
  };

  const toggleStatus = async (u: User) => {
    await authFetch(`${adminConfig.USERS_URL}/${u.id}/status`, { method: "PUT" });
    fetchUsers();
  };

  if (loading) return <p className="py-4 text-muted-foreground">加载中...</p>;

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold">用户列表</h3>
      <div className="bg-card border border-border/40 rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border/40 bg-muted/50">
              <th className="text-left px-4 py-2.5 font-medium">用户名</th>
              <th className="text-left px-4 py-2.5 font-medium">邮箱</th>
              <th className="text-left px-4 py-2.5 font-medium">手机号</th>
              <th className="text-left px-4 py-2.5 font-medium">角色</th>
              <th className="text-left px-4 py-2.5 font-medium">状态</th>
              <th className="text-right px-4 py-2.5 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            {users.length === 0 ? (
              <tr><td colSpan={6} className="px-4 py-6 text-center text-muted-foreground">暂无用户</td></tr>
            ) : users.map((u) => (
              <tr key={u.id} className="border-b border-border/20 last:border-0">
                <td className="px-4 py-2.5">{u.username}</td>
                <td className="px-4 py-2.5 text-muted-foreground">{u.email}</td>
                <td className="px-4 py-2.5 text-muted-foreground">{u.phoneNumber || "-"}</td>
                <td className="px-4 py-2.5">
                  <span className={`text-xs px-2 py-0.5 rounded-full ${u.roleId === 0 ? "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400" : "bg-gray-100 text-gray-500 dark:bg-gray-800 dark:text-gray-400"}`}>
                    {u.roleId === 0 ? "管理员" : "用户"}
                  </span>
                </td>
                <td className="px-4 py-2.5">
                  <span className={`text-xs px-2 py-0.5 rounded-full ${u.enabled ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400" : "bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400"}`}>
                    {u.enabled ? "启用" : "禁用"}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-right space-x-1">
                  <Button variant="ghost" size="sm" className="h-7 text-xs" onClick={() => toggleRole(u)}>
                    {u.roleId === 0 ? "设为用户" : "设为管理员"}
                  </Button>
                  <Button variant="ghost" size="sm" className={`h-7 text-xs ${u.enabled ? "text-destructive" : ""}`} onClick={() => toggleStatus(u)}>
                    {u.enabled ? "禁用" : "启用"}
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
