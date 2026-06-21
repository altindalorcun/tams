import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Trash2, KeyRound } from "lucide-react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import {
  listUsers,
  createUser,
  updateUser,
  deleteUser,
  resetPassword,
} from "@/api/userApi";
import type { UserResponse, CreateUserRequest, UpdateUserRequest, UserRole } from "@/types";

// ── Schemas ──────────────────────────────────────────────────────────────────

const createSchema = z
  .object({
    username: z.string().min(3, "Kullanıcı adı en az 3 karakter olmalıdır").max(100),
    email: z.string().email("Geçerli bir e-posta adresi girin"),
    role: z.enum(["TEACHER", "STUDENT"] as const),
    studentNumber: z.string().max(20).optional(),
  })
  .refine(
    (data) => {
      if (data.role === "STUDENT") return !!data.studentNumber?.trim();
      return true;
    },
    { message: "Öğrenci numarası zorunludur", path: ["studentNumber"] },
  );

const updateSchema = z.object({
  username: z.string().min(3, "Kullanıcı adı en az 3 karakter olmalıdır").max(100),
  email: z.string().email("Geçerli bir e-posta adresi girin"),
  isActive: z.boolean(),
});

type CreateFormValues = z.infer<typeof createSchema>;
type UpdateFormValues = z.infer<typeof updateSchema>;

// ── Helpers ───────────────────────────────────────────────────────────────────

const ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: "Yönetici",
  TEACHER: "Öğretim Üyesi",
  STUDENT: "Öğrenci",
};

function RoleBadge({ role }: { role: UserRole }) {
  const variant =
    role === "ADMIN" ? "default" : role === "TEACHER" ? "secondary" : "outline";
  return <Badge variant={variant}>{ROLE_LABELS[role]}</Badge>;
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString("tr-TR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

// ── Create Dialog ─────────────────────────────────────────────────────────────

interface CreateDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onSave: (data: CreateUserRequest) => Promise<void>;
}

function CreateDialog({ open, onOpenChange, onSave }: CreateDialogProps) {
  const form = useForm<CreateFormValues>({
    resolver: zodResolver(createSchema),
    defaultValues: { username: "", email: "", role: "TEACHER", studentNumber: "" },
  });

  const watchedRole = form.watch("role");

  async function onSubmit(values: CreateFormValues) {
    await onSave({
      username: values.username,
      email: values.email,
      role: values.role,
      studentNumber: values.role === "STUDENT" ? values.studentNumber : undefined,
    });
    form.reset();
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md shadow-xl">
        <DialogHeader>
          <DialogTitle>Yeni Kullanıcı Ekle</DialogTitle>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 pt-2">
            <FormField
              control={form.control}
              name="username"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Kullanıcı Adı</FormLabel>
                  <FormControl>
                    <Input placeholder="ornek.kullanici" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>E-posta</FormLabel>
                  <FormControl>
                    <Input type="email" placeholder="kullanici@hacettepe.edu.tr" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormItem>
              <FormLabel>Rol</FormLabel>
              <Controller
                control={form.control}
                name="role"
                render={({ field }) => (
                  <select
                    {...field}
                    className="flex h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <option value="TEACHER">Öğretim Üyesi</option>
                    <option value="STUDENT">Öğrenci</option>
                  </select>
                )}
              />
            </FormItem>
            {watchedRole === "STUDENT" && (
              <FormField
                control={form.control}
                name="studentNumber"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Öğrenci Numarası</FormLabel>
                    <FormControl>
                      <Input placeholder="B2312345" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            )}
            <p className="text-xs text-muted-foreground">
              Kullanıcı sisteme ilk girişinde şifresini değiştirmek zorunda kalacaktır.
            </p>
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                className="transition-colors duration-150"
              >
                İptal
              </Button>
              <Button
                type="submit"
                disabled={form.formState.isSubmitting}
                className="transition-colors duration-150"
              >
                Kaydet
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

// ── Edit Dialog ───────────────────────────────────────────────────────────────

interface EditDialogProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  user: UserResponse;
  onSave: (data: UpdateUserRequest) => Promise<void>;
}

function EditDialog({ open, onOpenChange, user, onSave }: EditDialogProps) {
  const form = useForm<UpdateFormValues>({
    resolver: zodResolver(updateSchema),
    values: { username: user.username, email: user.email, isActive: user.isActive },
  });

  async function onSubmit(values: UpdateFormValues) {
    await onSave(values);
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md shadow-xl">
        <DialogHeader>
          <DialogTitle>Kullanıcıyı Düzenle</DialogTitle>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 pt-2">
            <FormField
              control={form.control}
              name="username"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Kullanıcı Adı</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>E-posta</FormLabel>
                  <FormControl>
                    <Input type="email" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormItem>
              <FormLabel>Durum</FormLabel>
              <Controller
                control={form.control}
                name="isActive"
                render={({ field }) => (
                  <select
                    value={field.value ? "true" : "false"}
                    onChange={(e) => field.onChange(e.target.value === "true")}
                    className="flex h-8 w-full rounded-lg border border-input bg-transparent px-2.5 py-1 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <option value="true">Aktif</option>
                    <option value="false">Pasif</option>
                  </select>
                )}
              />
            </FormItem>
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                className="transition-colors duration-150"
              >
                İptal
              </Button>
              <Button
                type="submit"
                disabled={form.formState.isSubmitting}
                className="transition-colors duration-150"
              >
                Güncelle
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}

// ── UsersPage ─────────────────────────────────────────────────────────────────

/**
 * Admin user management page.
 * Lists all non-admin platform users with options to create, edit,
 * delete, and reset passwords.
 */
export function UsersPage() {
  const queryClient = useQueryClient();

  const [createOpen, setCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<UserResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<UserResponse | null>(null);
  const [resetTarget, setResetTarget] = useState<UserResponse | null>(null);

  const { data: users, isLoading, isError } = useQuery({
    queryKey: ["admin-users"],
    queryFn: listUsers,
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateUserRequest) => createUser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      toast.success("Kullanıcı oluşturuldu.");
    },
    onError: () => toast.error("Kullanıcı oluşturulamadı."),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserRequest }) =>
      updateUser(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      toast.success("Kullanıcı güncellendi.");
    },
    onError: () => toast.error("Güncelleme başarısız."),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      toast.success("Kullanıcı silindi.");
    },
    onError: () => toast.error("Silme işlemi başarısız."),
  });

  const resetMutation = useMutation({
    mutationFn: (id: string) => resetPassword(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      toast.success("Şifre sıfırlandı.");
    },
    onError: () => toast.error("Şifre sıfırlama başarısız."),
  });

  return (
    <div className="max-w-7xl mx-auto px-6 py-8 space-y-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Kullanıcı Yönetimi</h1>
        <Button
          onClick={() => setCreateOpen(true)}
          className="transition-colors duration-150"
        >
          <Plus className="mr-2 h-4 w-4" />
          Kullanıcı Ekle
        </Button>
      </div>

      {isLoading && (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-md" />
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">
          Kullanıcılar yüklenirken bir hata oluştu.
        </p>
      )}

      {users && users.length === 0 && (
        <div className="flex flex-col items-center justify-center gap-3 py-16 text-center">
          <p className="text-sm font-medium">Henüz kullanıcı yok</p>
          <p className="text-sm text-muted-foreground">
            Yeni kullanıcı eklemek için "Kullanıcı Ekle" butonunu kullanın.
          </p>
        </div>
      )}

      {users && users.length > 0 && (
        <div className="rounded-lg border shadow-sm overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Kullanıcı Adı</TableHead>
                <TableHead>E-posta</TableHead>
                <TableHead>Rol</TableHead>
                <TableHead>Öğrenci No</TableHead>
                <TableHead>Durum</TableHead>
                <TableHead>Kayıt Tarihi</TableHead>
                <TableHead className="text-right">İşlemler</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {users.map((user) => (
                <TableRow
                  key={user.id}
                  className="hover:bg-muted/50 transition-colors duration-150"
                >
                  <TableCell className="font-medium">{user.username}</TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {user.email}
                  </TableCell>
                  <TableCell>
                    <RoleBadge role={user.role} />
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {user.studentNumber ?? "—"}
                  </TableCell>
                  <TableCell>
                    {user.isActive ? (
                      <Badge variant="secondary">Aktif</Badge>
                    ) : (
                      <Badge variant="outline">Pasif</Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatDate(user.createdAt)}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        title="Düzenle"
                        onClick={() => setEditTarget(user)}
                        className="transition-colors duration-150"
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        title="Şifreyi Sıfırla"
                        onClick={() => setResetTarget(user)}
                        className="transition-colors duration-150"
                      >
                        <KeyRound className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        title="Sil"
                        onClick={() => setDeleteTarget(user)}
                        className="text-destructive hover:text-destructive transition-colors duration-150"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Create dialog */}
      <CreateDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        onSave={async (data) => { await createMutation.mutateAsync(data); }}
      />

      {/* Edit dialog */}
      {editTarget && (
        <EditDialog
          open={!!editTarget}
          onOpenChange={(v) => !v && setEditTarget(null)}
          user={editTarget}
          onSave={async (data) => { await updateMutation.mutateAsync({ id: editTarget.id, data }); }}
        />
      )}

      {/* Delete confirmation */}
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(v) => !v && setDeleteTarget(null)}
        title="Kullanıcıyı Sil"
        description={
          deleteTarget
            ? `"${deleteTarget.username}" kullanıcısını kalıcı olarak silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.`
            : ""
        }
        confirmLabel="Sil"
        onConfirm={() => {
          if (deleteTarget) {
            deleteMutation.mutate(deleteTarget.id);
            setDeleteTarget(null);
          }
        }}
      />

      {/* Reset password confirmation */}
      <ConfirmDialog
        open={!!resetTarget}
        onOpenChange={(v) => !v && setResetTarget(null)}
        title="Şifreyi Sıfırla"
        description={
          resetTarget
            ? `"${resetTarget.username}" kullanıcısının şifresi varsayılan değere sıfırlanacak ve bir sonraki girişte şifre değiştirmesi istenecek.`
            : ""
        }
        confirmLabel="Sıfırla"
        onConfirm={() => {
          if (resetTarget) {
            resetMutation.mutate(resetTarget.id);
            setResetTarget(null);
          }
        }}
      />
    </div>
  );
}
