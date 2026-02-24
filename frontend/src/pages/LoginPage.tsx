import Login from "@/modules/features/auth/components/LoginForm";
import Register from "@/modules/features/auth/components/RegisterForm";
import { useNavigate } from "react-router-dom";
import { Button } from "@/ui/button";

export default function LoginPage({
  onLoginSuccess,
  onRegisterSuccess,
  showRegister,
  setShowRegister,
}: {
  onLoginSuccess: () => void;
  onRegisterSuccess: () => void;
  showRegister: boolean;
  setShowRegister: (v: boolean) => void;
}) {
  const navigate = useNavigate();
  return (
    <div>
      {showRegister ? (
        <Register onRegisterSuccess={onRegisterSuccess} />
      ) : (
        <Login
          onLoginSuccess={() => {
            onLoginSuccess();
            navigate("/");
          }}
        />
      )}
      <div className="fixed bottom-8 left-1/2 transform -translate-x-1/2">
        <Button
          variant="outline"
          className="px-6 py-3 rounded-full bg-card/90 backdrop-blur-sm border-border/50 text-foreground hover:bg-card hover:shadow-lg shadow-sm transition-all duration-200 font-medium"
          onClick={() => setShowRegister(!showRegister)}
        >
          {showRegister
            ? "已有账号？去登录"
            : "还没有账号？去注册"}
        </Button>
      </div>
    </div>
  );
}
