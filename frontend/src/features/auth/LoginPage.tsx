import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate } from "react-router-dom";
import { Loader2, TriangleAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { PasswordInput } from "@/components/PasswordInput";
import { LogoFull } from "@/components/brand/LogoFull";
import { HacettepeLogo } from "@/components/brand/HacettepeLogo";
import { useAuthStore } from "./authStore";
import { login } from "@/api/authApi";

const LOGIN_INVALID_CREDENTIALS_MESSAGE = "E-posta veya şifre hatalı.";

const loginSchema = z.object({
  email: z.string().min(1, "E-posta gereklidir"),
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
  const [loginError, setLoginError] = useState<string | null>(null);

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

  function clearLoginError() {
    setLoginError(null);
  }

  async function onSubmit(values: LoginFormValues) {
    setLoginError(null);
    try {
      const response = await login(values);
      setAuth(response.accessToken, response.mustChangePassword, response.username);
    } catch {
      setLoginError(LOGIN_INVALID_CREDENTIALS_MESSAGE);
    }
  }

  return (
    <div className="flex min-h-screen">
      {/* Left panel — brand */}
      <section className="hidden md:flex md:w-1/2 flex-col items-center justify-center bg-muted/30 px-12 py-16 gap-8">
        <LogoFull />
      </section>

      {/* Right panel — form */}
      <section className="flex flex-1 flex-col items-center justify-center px-8 py-12 md:px-16 lg:px-24">
        <div className="max-w-sm w-full mx-auto">
          {/* Logo shown on mobile only */}
          <div className="flex justify-center mb-10 md:hidden">
            <LogoFull />
          </div>

          <HacettepeLogo className="mx-auto mb-6 max-w-[80px] opacity-100" />

          <h1 className="text-2xl font-semibold mb-2">Giriş Yap</h1>
          <p className="text-sm text-muted-foreground mb-8">
            Hesabınıza erişmek için e-posta adresinizi ve şifrenizi girin.
          </p>

          {loginError && (
            <Alert variant="destructive" className="mb-4">
              <TriangleAlert className="h-4 w-4" />
              <AlertDescription>{loginError}</AlertDescription>
            </Alert>
          )}

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4" noValidate>
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>E-posta</FormLabel>
                    <FormControl>
                      <Input
                        type="email"
                        placeholder="kullanici@hacettepe.edu.tr"
                        autoComplete="username"
                        {...field}
                        onChange={(event) => {
                          clearLoginError();
                          field.onChange(event);
                        }}
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
                      <PasswordInput
                        placeholder="••••••••"
                        autoComplete="current-password"
                        {...field}
                        onChange={(event) => {
                          clearLoginError();
                          field.onChange(event);
                        }}
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
      </section>
    </div>
  );
}
