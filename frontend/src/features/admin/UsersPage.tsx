import { useMemo, useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Pencil, Trash2, KeyRound, Filter } from "lucide-react";
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
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
import { Popover, PopoverContent, PopoverHeader, PopoverTitle, PopoverTrigger } from "@/components/ui/popover";
import { matchesTextFilter } from "@/lib/textFilter";
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

const FILTERABLE_USER_ROLES = ["TEACHER", "STUDENT"] as const;
type UserRoleFilter = (typeof FILTERABLE_USER_ROLES)[number];

const USER_STATUS_ACTIVE = "ACTIVE" as const;
const USER_STATUS_INACTIVE = "INACTIVE" as const;
type UserStatusFilter = typeof USER_STATUS_ACTIVE | typeof USER_STATUS_INACTIVE;

const STATUS_FILTER_LABELS: Record<UserStatusFilter, string> = {
  ACTIVE: "Aktif",
  INACTIVE: "Pasif",
};

const ROLE_FILTER_ITEMS = FILTERABLE_USER_ROLES.map((role) => ({
  value: role,
  label: ROLE_LABELS[role],
}));

const STATUS_FILTER_ITEMS = (
  [USER_STATUS_ACTIVE, USER_STATUS_INACTIVE] as const
).map((status) => ({
  value: status,
  label: STATUS_FILTER_LABELS[status],
}));

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
  const [usernameFilter, setUsernameFilter] = useState("");
  const [roleFilter, setRoleFilter] = useState<UserRoleFilter | null>(null);
  const [studentNumberFilter, setStudentNumberFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState<UserStatusFilter | null>(null);

  const { data: users, isLoading, isError } = useQuery({
    queryKey: ["admin-users"],
    queryFn: listUsers,
  });

  const filteredUsers = useMemo(() => {
    return (users ?? []).filter((user) => {
      if (!matchesTextFilter(user.username, usernameFilter)) return false;
      if (roleFilter !== null && user.role !== roleFilter) return false;
      if (!matchesTextFilter(user.studentNumber ?? "", studentNumberFilter)) return false;
      if (statusFilter === USER_STATUS_ACTIVE && !user.isActive) return false;
      if (statusFilter === USER_STATUS_INACTIVE && user.isActive) return false;
      return true;
    });
  }, [users, usernameFilter, roleFilter, studentNumberFilter, statusFilter]);

  const hasActiveFilters =
    usernameFilter.trim() !== ""
    || roleFilter !== null
    || studentNumberFilter.trim() !== ""
    || statusFilter !== null;

  const activeFilterCount = [
    usernameFilter.trim() !== "",
    roleFilter !== null,
    studentNumberFilter.trim() !== "",
    statusFilter !== null,
  ].filter(Boolean).length;

  function clearFilters() {
    setUsernameFilter("");
    setRoleFilter(null);
    setStudentNumberFilter("");
    setStatusFilter(null);
  }

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
        <div className="flex items-center gap-2">
          {!isLoading && !isError && (users?.length ?? 0) > 0 && (
            <Popover>
              <PopoverTrigger
                render={
                  <Button
                    variant="outline"
                    size="sm"
                    className="transition-colors duration-150"
                    aria-pressed={hasActiveFilters}
                    aria-label="Kullanıcıları filtrele"
                  />
                }
              >
                <Filter className="mr-1 h-4 w-4" />
                Filtre
                {hasActiveFilters && (
                  <Badge variant="secondary" className="ml-1.5 h-5 min-w-5 px-1.5 text-xs">
                    {activeFilterCount}
                  </Badge>
                )}
              </PopoverTrigger>
              <PopoverContent align="end" className="w-80 shadow-md">
                <PopoverHeader>
                  <PopoverTitle>Kullanıcıları Filtrele</PopoverTitle>
                </PopoverHeader>
                <div className="flex flex-col gap-3">
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="users-username-filter" className="text-sm font-medium text-muted-foreground">
                      Kullanıcı Adı
                    </label>
                    <Input
                      id="users-username-filter"
                      placeholder="Kullanıcı adına göre filtrele"
                      value={usernameFilter}
                      onChange={(e) => setUsernameFilter(e.target.value)}
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="users-role-filter" className="text-sm font-medium text-muted-foreground">
                      Rol
                    </label>
                    <Select
                      items={ROLE_FILTER_ITEMS}
                      value={roleFilter}
                      onValueChange={setRoleFilter}
                    >
                      <SelectTrigger id="users-role-filter" className="w-full">
                        <SelectValue placeholder="Rol seçin" />
                      </SelectTrigger>
                      <SelectContent>
                        {FILTERABLE_USER_ROLES.map((role) => (
                          <SelectItem key={role} value={role}>
                            {ROLE_LABELS[role]}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="users-student-number-filter" className="text-sm font-medium text-muted-foreground">
                      Öğrenci No
                    </label>
                    <Input
                      id="users-student-number-filter"
                      className="font-mono"
                      placeholder="Öğrenci numarasına göre filtrele"
                      value={studentNumberFilter}
                      onChange={(e) => setStudentNumberFilter(e.target.value)}
                    />
                  </div>
                  <div className="flex flex-col gap-1.5">
                    <label htmlFor="users-status-filter" className="text-sm font-medium text-muted-foreground">
                      Durum
                    </label>
                    <Select
                      items={STATUS_FILTER_ITEMS}
                      value={statusFilter}
                      onValueChange={setStatusFilter}
                    >
                      <SelectTrigger id="users-status-filter" className="w-full">
                        <SelectValue placeholder="Durum seçin" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value={USER_STATUS_ACTIVE}>
                          {STATUS_FILTER_LABELS.ACTIVE}
                        </SelectItem>
                        <SelectItem value={USER_STATUS_INACTIVE}>
                          {STATUS_FILTER_LABELS.INACTIVE}
                        </SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  {hasActiveFilters && (
                    <Button variant="ghost" onClick={clearFilters} className="self-start transition-colors duration-150">
                      Temizle
                    </Button>
                  )}
                </div>
              </PopoverContent>
            </Popover>
          )}
          <Button
            onClick={() => setCreateOpen(true)}
            className="transition-colors duration-150"
          >
            <Plus className="mr-2 h-4 w-4" />
            Kullanıcı Ekle
          </Button>
        </div>
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

      {users && users.length > 0 && filteredUsers.length === 0 && (
        <p className="text-sm text-muted-foreground text-center py-8">
          Filtreye uygun kullanıcı bulunamadı.
        </p>
      )}

      {users && users.length > 0 && filteredUsers.length > 0 && (
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
              {filteredUsers.map((user) => (
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
