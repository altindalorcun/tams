import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate } from "react-router-dom";
import { Loader2, ShieldCheck } from "lucide-react";
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
import { changePassword } from "@/api/authApi";

const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "Mevcut şifre gereklidir"),
    newPassword: z
      .string()
      .min(8, "Yeni şifre en az 8 karakter olmalıdır")
      .max(128, "Yeni şifre en fazla 128 karakter olabilir"),
    confirmPassword: z.string().min(1, "Şifre onayı gereklidir"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Şifreler eşleşmiyor",
    path: ["confirmPassword"],
  });

type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;

/**
 * Mandatory password change page shown on first login.
 * After a successful change, all sessions are invalidated server-side
 * and the user is redirected to the login page to sign in with the new password.
 */
export function ChangePasswordPage() {
  const { accessToken, mustChangePassword, clearAuth } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (!accessToken) {
      navigate("/login", { replace: true });
    }
  }, [accessToken, navigate]);

  const form = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });

  const { isSubmitting } = form.formState;

  async function onSubmit(values: ChangePasswordFormValues) {
    try {
      await changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      clearAuth();
      toast.success("Şifreniz başarıyla değiştirildi. Lütfen yeniden giriş yapın.");
      navigate("/login", { replace: true });
    } catch {
      toast.error("Şifre değiştirilemedi. Mevcut şifrenizi kontrol edin.");
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

          <div className="flex items-center gap-2 mb-2">
            <ShieldCheck className="h-5 w-5 text-primary" />
            <h1 className="text-2xl font-semibold">Şifre Oluştur</h1>
          </div>

          {mustChangePassword ? (
            <p className="text-sm text-muted-foreground mb-8">
              Güvenliğiniz için sisteme ilk girişte şifrenizi değiştirmeniz zorunludur.
              Mevcut şifrenizi ve yeni şifrenizi girerek devam edin.
            </p>
          ) : (
            <p className="text-sm text-muted-foreground mb-8">
              Şifrenizi değiştirmek için mevcut ve yeni şifrenizi girin.
              İşlem sonrasında yeniden giriş yapmanız istenecektir.
            </p>
          )}

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4" noValidate>
              <FormField
                control={form.control}
                name="currentPassword"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Mevcut Şifre</FormLabel>
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
              <FormField
                control={form.control}
                name="newPassword"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Yeni Şifre</FormLabel>
                    <FormControl>
                      <Input
                        type="password"
                        placeholder="En az 8 karakter"
                        autoComplete="new-password"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="confirmPassword"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Yeni Şifre (Tekrar)</FormLabel>
                    <FormControl>
                      <Input
                        type="password"
                        placeholder="••••••••"
                        autoComplete="new-password"
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
                    Kaydediliyor…
                  </>
                ) : (
                  "Şifreyi Değiştir"
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
