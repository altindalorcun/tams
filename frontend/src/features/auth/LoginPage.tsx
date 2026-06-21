import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { LogoFull } from "@/components/brand/LogoFull";
import { HacettepeLogo } from "@/components/brand/HacettepeLogo";
import { useAuthStore } from "./authStore";
import { login } from "@/api/authApi";

const loginSchema = z.object({
  email: z.string().min(1, "E-posta veya kullanıcı adı gereklidir"),
  password: z.string().min(1, "Şifre gereklidir"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

/**
 * Full-screen login page.
 * Desktop: two-column layout (brand left, form right).
 * Mobile: single-column with form.
 */
export function LoginPage() {
  const { accessToken, role, mustChangePassword, setAuth } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (!accessToken || !role) return;
    if (mustChangePassword) {
      navigate("/change-password", { replace: true });
      return;
    }
    const destination =
      role === "ADMIN" ? "/admin" : role === "TEACHER" ? "/teacher" : "/student/results";
    navigate(destination, { replace: true });
  }, [accessToken, role, mustChangePassword, navigate]);

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  });

  const { isSubmitting } = form.formState;

  async function onSubmit(values: LoginFormValues) {
    try {
      const response = await login(values);
      setAuth(response.accessToken, response.mustChangePassword);
    } catch {
      toast.error("Giriş başarısız. E-posta/kullanıcı adı veya şifrenizi kontrol edin.");
    }
  }

  return (
    <div className="flex min-h-screen">
      {/* Left panel — brand */}
      <section className="hidden md:flex md:w-1/2 flex-col items-center justify-center bg-muted/30 px-12 py-16 gap-8">
        <LogoFull />
      </section>

      {/* Right panel — form */}
      <section className="flex flex-1 flex-col justify-between px-8 py-12 md:px-16 lg:px-24">
        <div className="flex flex-col justify-center flex-1 max-w-sm w-full mx-auto">
          {/* Logo shown on mobile only */}
          <div className="flex justify-center mb-10 md:hidden">
            <LogoFull />
          </div>

          <h1 className="text-2xl font-semibold mb-2">Giriş Yap</h1>
          <p className="text-sm text-muted-foreground mb-8">
            Hesabınıza erişmek için e-posta/kullanıcı adı ve şifrenizi girin.
          </p>

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4" noValidate>
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>E-posta veya Kullanıcı Adı</FormLabel>
                    <FormControl>
                      <Input
                        type="text"
                        placeholder="kullanici@hacettepe.edu.tr"
                        autoComplete="username"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Şifre</FormLabel>
                    <FormControl>
                      <Input
                        type="password"
                        placeholder="••••••••"
                        autoComplete="current-password"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <Button
                type="submit"
                className="w-full mt-2 transition-colors duration-150"
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Giriş yapılıyor…
                  </>
                ) : (
                  "Giriş Yap"
                )}
              </Button>
            </form>
          </Form>
        </div>

        {/* Hacettepe logo bottom-left */}
        <div className="flex items-end">
          <HacettepeLogo />
        </div>
      </section>
    </div>
  );
}
