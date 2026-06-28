import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./ProtectedRoute";
import { AppShell } from "@/components/AppShell";
import { LoginPage } from "@/features/auth/LoginPage";
import { ChangePasswordPage } from "@/features/auth/ChangePasswordPage";
import { AdminPage } from "@/features/admin/AdminPage";
import { CategoriesPage } from "@/features/admin/CategoriesPage";
import { CoursesPage } from "@/features/admin/CoursesPage";
import { DepartmentsPage } from "@/features/admin/DepartmentsPage";
import { CurriculumEquivalenceRulesPage } from "@/features/admin/CurriculumEquivalenceRulesPage";
import { UsersPage } from "@/features/admin/UsersPage";
import { TeacherPage } from "@/features/teacher/TeacherPage";
import { StudentHistoryPage } from "@/features/teacher/StudentHistoryPage";
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
 * /change-password is protected (requires auth token) but rendered without AppShell
 * so the user cannot navigate away before completing the mandatory password change.
 */
export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/forbidden" element={<ForbiddenPage />} />

        {/* Mandatory password change — requires auth token, no AppShell */}
        <Route element={<ProtectedRoute allowMustChangePassword />}>
          <Route path="/change-password" element={<ChangePasswordPage />} />
        </Route>

        {/* Authenticated shell */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route path="/admin/*" element={<ProtectedRoute requiredRole="ADMIN" />}>
              <Route index element={<AdminPage />} />
              <Route path="departments" element={<DepartmentsPage />} />
              <Route path="courses" element={<CoursesPage />} />
              <Route path="graduation-categories" element={<CategoriesPage />} />
              <Route path="curriculum-equivalence-rules" element={<CurriculumEquivalenceRulesPage />} />
              <Route path="users" element={<UsersPage />} />
            </Route>
            <Route path="/teacher/*" element={<ProtectedRoute requiredRole="TEACHER" />}>
              <Route index element={<TeacherPage />} />
              <Route path="history" element={<StudentHistoryPage />} />
            </Route>
            <Route path="/student/*" element={<ProtectedRoute requiredRole="STUDENT" />}>
              <Route path="results" element={<StudentResultPage />} />
              <Route index element={<StudentResultPage />} />
            </Route>
            <Route path="/" element={<Navigate to="/login" replace />} />
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
