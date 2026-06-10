import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./ProtectedRoute";
import { AppShell } from "@/components/AppShell";
import { LoginPage } from "@/features/auth/LoginPage";
import { AdminPage } from "@/features/admin/AdminPage";
import { TeacherPage } from "@/features/teacher/TeacherPage";
import { StudentResultPage } from "@/features/student/StudentResultPage";

/** Lazy-loaded placeholder pages — will be replaced by feature implementations */
function ForbiddenPage() {
  return (
    <main className="flex h-full flex-col items-center justify-center gap-4 px-6 py-8">
      <h1 className="text-2xl font-semibold">Yetkisiz Erişim</h1>
      <p className="text-muted-foreground text-sm">Bu sayfaya erişim yetkiniz bulunmamaktadır.</p>
    </main>
  );
}

/**
 * Top-level router.
 * Protected areas are nested under AppShell via the ProtectedRoute wrapper.
 */
export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/forbidden" element={<ForbiddenPage />} />

        {/* Authenticated shell */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route path="/admin/*" element={<ProtectedRoute requiredRole="ADMIN" />}>
              <Route index element={<AdminPage />} />
            </Route>
            <Route path="/teacher/*" element={<ProtectedRoute requiredRole="TEACHER" />}>
              <Route index element={<TeacherPage />} />
            </Route>
            <Route path="/student/*" element={<ProtectedRoute requiredRole="STUDENT" />}>
              <Route path="results" element={<StudentResultPage />} />
              <Route index element={<StudentResultPage />} />
            </Route>
            {/* Default authenticated landing — redirect based on role is handled in AppShell */}
            <Route path="/" element={<Navigate to="/login" replace />} />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
