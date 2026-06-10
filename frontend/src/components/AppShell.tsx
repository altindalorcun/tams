import { useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import {
  BookOpen,
  GraduationCap,
  LayoutDashboard,
  LogOut,
  Menu,
  Settings,
  Upload,
  Users,
} from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Separator } from "@/components/ui/separator";
import { Sheet, SheetContent } from "@/components/ui/sheet";
import { LogoIcon } from "@/components/brand/LogoIcon";
import { ThemeToggle } from "@/components/ThemeToggle";
import { useAuthStore } from "@/features/auth/authStore";
import { cn } from "@/lib/utils";
import type { UserRole } from "@/types";

interface NavItem {
  label: string;
  to: string;
  icon: React.ReactNode;
}

const NAV_ITEMS: Record<UserRole, NavItem[]> = {
  ADMIN: [
    { label: "Genel Bakış", to: "/admin", icon: <LayoutDashboard className="h-4 w-4" /> },
    { label: "Bölümler", to: "/admin/departments", icon: <Settings className="h-4 w-4" /> },
    { label: "Dersler", to: "/admin/courses", icon: <BookOpen className="h-4 w-4" /> },
    { label: "Kullanıcılar", to: "/admin/users", icon: <Users className="h-4 w-4" /> },
  ],
  TEACHER: [
    { label: "Transkript Yükle", to: "/teacher", icon: <Upload className="h-4 w-4" /> },
    { label: "Öğrenci Geçmişi", to: "/teacher/history", icon: <GraduationCap className="h-4 w-4" /> },
  ],
  STUDENT: [
    { label: "Mezuniyet Durumu", to: "/student/results", icon: <GraduationCap className="h-4 w-4" /> },
  ],
};

interface SidebarNavProps {
  role: UserRole;
  collapsed: boolean;
}

function SidebarNav({ role, collapsed }: SidebarNavProps) {
  const items = NAV_ITEMS[role] ?? [];

  return (
    <nav aria-label="Ana navigasyon" className="flex flex-col gap-1 px-2">
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.to === "/admin" || item.to === "/teacher"}
          className={({ isActive }) =>
            cn(
              "flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors duration-150",
              "hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
              isActive
                ? "bg-primary/10 text-primary font-medium"
                : "text-foreground/80",
              collapsed && "justify-center px-2",
            )
          }
          title={collapsed ? item.label : undefined}
        >
          {item.icon}
          {!collapsed && <span>{item.label}</span>}
        </NavLink>
      ))}
    </nav>
  );
}

interface SidebarContentProps {
  role: UserRole;
  collapsed: boolean;
}

function SidebarContent({ role, collapsed }: SidebarContentProps) {
  return (
    <div className="flex h-full flex-col">
      <div className={cn("flex items-center gap-3 px-4 py-4", collapsed && "justify-center px-2")}>
        <LogoIcon />
        {!collapsed && (
          <span className="text-sm font-semibold tracking-tight text-foreground">TAMS</span>
        )}
      </div>
      <Separator />
      <div className="flex-1 overflow-y-auto py-4">
        <SidebarNav role={role} collapsed={collapsed} />
      </div>
    </div>
  );
}

/**
 * Root layout for authenticated users.
 * Desktop: collapsible left sidebar. Mobile: Sheet/drawer navigation.
 */
export function AppShell() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const { role, userId, clearAuth } = useAuthStore();
  const navigate = useNavigate();

  function handleLogout() {
    clearAuth();
    navigate("/login", { replace: true });
  }

  const userRole = role ?? "STUDENT";
  const userInitial = userId ? userId.slice(0, 2).toUpperCase() : "?";

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      {/* Desktop sidebar */}
      <aside
        className={cn(
          "hidden md:flex flex-col border-r bg-sidebar transition-[width] duration-150",
          collapsed ? "w-16" : "w-60",
        )}
      >
        <SidebarContent role={userRole} collapsed={collapsed} />
      </aside>

      {/* Mobile sidebar — Sheet drawer controlled by state */}
      <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
        <SheetContent side="left" className="w-60 p-0 bg-sidebar" showCloseButton={false}>
          <SidebarContent role={userRole} collapsed={false} />
        </SheetContent>
      </Sheet>

      {/* Main area */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Top header */}
        <header className="flex h-14 shrink-0 items-center justify-between border-b bg-background px-4">
          <div className="flex items-center gap-2">
            {/* Mobile: open drawer */}
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden transition-colors duration-150"
              aria-label="Menüyü aç"
              onClick={() => setMobileOpen(true)}
            >
              <Menu className="h-4 w-4" />
            </Button>

            {/* Desktop: collapse sidebar */}
            <Button
              variant="ghost"
              size="icon"
              className="hidden md:flex transition-colors duration-150"
              onClick={() => setCollapsed((c) => !c)}
              aria-label={collapsed ? "Kenar çubuğunu genişlet" : "Kenar çubuğunu daralt"}
            >
              <Menu className="h-4 w-4" />
            </Button>
          </div>

          <div className="flex items-center gap-2">
            <ThemeToggle />
            <DropdownMenu>
              <DropdownMenuTrigger
                render={
                  <button
                    className="rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring transition-colors duration-150"
                    aria-label="Kullanıcı menüsü"
                  />
                }
              >
                <Avatar className="h-8 w-8">
                  <AvatarFallback className="text-xs bg-primary/10 text-primary">
                    {userInitial}
                  </AvatarFallback>
                </Avatar>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48 shadow-md">
                <div className="px-2 py-1.5">
                  <p className="text-xs text-muted-foreground">
                    {userRole === "ADMIN"
                      ? "Yönetici"
                      : userRole === "TEACHER"
                        ? "Öğretmen"
                        : "Öğrenci"}
                  </p>
                </div>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={handleLogout}
                  className="text-destructive focus:text-destructive cursor-pointer transition-colors duration-150"
                >
                  <LogOut className="mr-2 h-4 w-4" />
                  Çıkış Yap
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
