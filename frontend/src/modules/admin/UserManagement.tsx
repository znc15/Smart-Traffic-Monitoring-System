import { useCallback, useEffect, useMemo, useState } from "react";
import { Button } from "@/ui/button";
import { Input } from "@/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/ui/select";
import { adminConfig, authFetch } from "@/config";
import { toast } from "sonner";
import { motion, AnimatePresence } from "framer-motion";
import { Search, Loader2, Check, Users, UserX } from "lucide-react";

type User = {
  id: number;
  username: string;
  email: string;
  phone_number: string;
  role_id: number;
  enabled: boolean;
  createdAt: string;
};

type RoleFilter = "all" | "admin" | "user";
type StatusFilter = "all" | "enabled" | "disabled";

// Per-button loading/success state
type ActionState = { loading: boolean; success: boolean };

export default function UserManagement() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  // Search & filter state
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("all");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");

  // Per-user action states: { [userId]: { role: ActionState, status: ActionState } }
  const [actionStates, setActionStates] = useState<
    Record<number, { role: ActionState; status: ActionState }>
  >({});

  const fetchUsers = useCallback(async () => {
    try {
      const res = await authFetch(adminConfig.USERS_URL);
      if (res.ok) setUsers(await res.json());
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  // Client-side filtering
  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      // Search by username or email
      const q = search.toLowerCase().trim();
      if (q && !u.username.toLowerCase().includes(q) && !u.email.toLowerCase().includes(q)) {
        return false;
      }
      // Role filter
      if (roleFilter === "admin" && u.role_id !== 0) return false;
      if (roleFilter === "user" && u.role_id !== 1) return false;
      // Status filter
      if (statusFilter === "enabled" && !u.enabled) return false;
      if (statusFilter === "disabled" && u.enabled) return false;
      return true;
    });
  }, [users, search, roleFilter, statusFilter]);

  const setActionState = (
    userId: number,
    key: "role" | "status",
    state: Partial<ActionState>
  ) => {
    setActionStates((prev) => ({
      ...prev,
      [userId]: {
        role: { loading: false, success: false },
        status: { loading: false, success: false },
        ...prev[userId],
        [key]: { ...prev[userId]?.[key], ...state },
      },
    }));
  };

  const toggleRole = async (u: User) => {
    setActionState(u.id, "role", { loading: true, success: false });
    try {
      const res = await authFetch(`${adminConfig.USERS_URL}/${u.id}/role`, {
        method: "PUT",
        body: JSON.stringify({ roleId: u.role_id === 0 ? 1 : 0 }),
      });
      if (res.ok) {
        setActionState(u.id, "role", { loading: false, success: true });
        toast.success("角色已更新");
        await fetchUsers();
        setTimeout(() => setActionState(u.id, "role", { success: false }), 1500);
      } else {
        const data = await res.json().catch(() => null);
        setActionState(u.id, "role", { loading: false });
        toast.error(data?.detail || "角色更新失败");
      }
    } catch {
      setActionState(u.id, "role", { loading: false });
      toast.error("网络错误，请稍后重试");
    }
  };

  const toggleStatus = async (u: User) => {
    setActionState(u.id, "status", { loading: true, success: false });
    try {
      const res = await authFetch(`${adminConfig.USERS_URL}/${u.id}/status`, {
        method: "PUT",
      });
      if (res.ok) {
        setActionState(u.id, "status", { loading: false, success: true });
        toast.success("状态已更新");
        await fetchUsers();
        setTimeout(() => setActionState(u.id, "status", { success: false }), 1500);
      } else {
        const data = await res.json().catch(() => null);
        setActionState(u.id, "status", { loading: false });
        toast.error(data?.detail || "状态更新失败");
      }
    } catch {
      setActionState(u.id, "status", { loading: false });
      toast.error("网络错误，请稍后重试");
    }
  };

  const getActionState = (userId: number, key: "role" | "status"): ActionState => {
    return actionStates[userId]?.[key] ?? { loading: false, success: false };
  };

  if (loading) return <p className="py-4 text-muted-foreground">加载中...</p>;

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold">用户列表</h3>

      {/* Toolbar: search + filters */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
          <Input
            placeholder="搜索用户名或邮箱..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
          />
        </div>
        <Select value={roleFilter} onValueChange={(v) => setRoleFilter(v as RoleFilter)}>
          <SelectTrigger className="w-full sm:w-[130px]">
            <SelectValue placeholder="角色筛选" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">全部角色</SelectItem>
            <SelectItem value="admin">管理员</SelectItem>
            <SelectItem value="user">用户</SelectItem>
          </SelectContent>
        </Select>
        <Select value={statusFilter} onValueChange={(v) => setStatusFilter(v as StatusFilter)}>
          <SelectTrigger className="w-full sm:w-[130px]">
            <SelectValue placeholder="状态筛选" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">全部状态</SelectItem>
            <SelectItem value="enabled">启用</SelectItem>
            <SelectItem value="disabled">禁用</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Table */}
      <div className="bg-card border border-border/40 rounded-lg overflow-hidden">
        <div className="max-h-[60vh] overflow-auto">
          <table className="w-full text-sm">
            <thead className="sticky top-0 z-10 bg-muted/80 backdrop-blur-sm">
              <tr className="border-b border-border/40">
                <th className="text-left px-4 py-2.5 font-medium">用户名</th>
                <th className="text-left px-4 py-2.5 font-medium hidden md:table-cell">邮箱</th>
                <th className="text-left px-4 py-2.5 font-medium hidden lg:table-cell">手机号</th>
                <th className="text-left px-4 py-2.5 font-medium">角色</th>
                <th className="text-left px-4 py-2.5 font-medium">状态</th>
                <th className="text-right px-4 py-2.5 font-medium">操作</th>
              </tr>
            </thead>
            <tbody>
              <AnimatePresence mode="popLayout">
                {filteredUsers.length === 0 ? (
                  <motion.tr
                    key="empty"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                  >
                    <td colSpan={6} className="px-4 py-12 text-center">
                      <div className="flex flex-col items-center gap-2 text-muted-foreground">
                        <UserX className="h-8 w-8 opacity-40" />
                        <span>{search || roleFilter !== "all" || statusFilter !== "all" ? "没有匹配的用户" : "暂无用户"}</span>
                      </div>
                    </td>
                  </motion.tr>
                ) : (
                  filteredUsers.map((u) => {
                    const roleState = getActionState(u.id, "role");
                    const statusState = getActionState(u.id, "status");
                    return (
                      <motion.tr
                        key={u.id}
                        layout
                        initial={{ opacity: 0, y: 8 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, x: -16 }}
                        transition={{ duration: 0.2 }}
                        className="border-b border-border/20 last:border-0 hover:bg-muted/40 transition-colors"
                      >
                        <td className="px-4 py-2.5 font-medium">{u.username}</td>
                        <td className="px-4 py-2.5 text-muted-foreground hidden md:table-cell">{u.email}</td>
                        <td className="px-4 py-2.5 text-muted-foreground hidden lg:table-cell">{u.phone_number || "-"}</td>
                        <td className="px-4 py-2.5">
                          <span
                            className={`inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full font-medium ${
                              u.role_id === 0
                                ? "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400"
                                : "bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400"
                            }`}
                          >
                            {u.role_id === 0 ? "管理员" : "用户"}
                          </span>
                        </td>
                        <td className="px-4 py-2.5">
                          <span
                            className={`inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full font-medium ${
                              u.enabled
                                ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400"
                                : "bg-red-100 text-red-600 dark:bg-red-900/30 dark:text-red-400"
                            }`}
                          >
                            {u.enabled ? "启用" : "禁用"}
                          </span>
                        </td>
                        <td className="px-4 py-2.5 text-right">
                          <div className="inline-flex gap-1">
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-7 text-xs min-w-[76px]"
                              disabled={roleState.loading}
                              onClick={() => toggleRole(u)}
                            >
                              <AnimatePresence mode="wait">
                                {roleState.loading ? (
                                  <motion.span key="loading" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="flex items-center gap-1">
                                    <Loader2 className="h-3 w-3 animate-spin" />
                                  </motion.span>
                                ) : roleState.success ? (
                                  <motion.span key="success" initial={{ scale: 0.5, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ opacity: 0 }} className="flex items-center gap-1 text-emerald-600">
                                    <Check className="h-3 w-3" />
                                  </motion.span>
                                ) : (
                                  <motion.span key="idle" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                                    {u.role_id === 0 ? "设为用户" : "设为管理员"}
                                  </motion.span>
                                )}
                              </AnimatePresence>
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              className={`h-7 text-xs min-w-[52px] ${!statusState.loading && !statusState.success && u.enabled ? "text-destructive" : ""}`}
                              disabled={statusState.loading}
                              onClick={() => toggleStatus(u)}
                            >
                              <AnimatePresence mode="wait">
                                {statusState.loading ? (
                                  <motion.span key="loading" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="flex items-center gap-1">
                                    <Loader2 className="h-3 w-3 animate-spin" />
                                  </motion.span>
                                ) : statusState.success ? (
                                  <motion.span key="success" initial={{ scale: 0.5, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ opacity: 0 }} className="flex items-center gap-1 text-emerald-600">
                                    <Check className="h-3 w-3" />
                                  </motion.span>
                                ) : (
                                  <motion.span key="idle" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                                    {u.enabled ? "禁用" : "启用"}
                                  </motion.span>
                                )}
                              </AnimatePresence>
                            </Button>
                          </div>
                        </td>
                      </motion.tr>
                    );
                  })
                )}
              </AnimatePresence>
            </tbody>
          </table>
        </div>
      </div>

      {/* Footer stats */}
      <div className="flex items-center gap-2 text-xs text-muted-foreground">
        <Users className="h-3.5 w-3.5" />
        <span>
          {filteredUsers.length === users.length
            ? `共 ${users.length} 位用户`
            : `显示 ${filteredUsers.length} / ${users.length} 位用户`}
        </span>
      </div>
    </div>
  );
}
