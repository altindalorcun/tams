import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import {
  BookOpen,
  ChevronDown,
  GraduationCap,
  KeyRound,
  LayoutDashboard,
  ListChecks,
  LogOut,
  Menu,
  Scale,
  Settings,
  Upload,
  User,
  Users,
} from "lucide-react";
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

/** Fallback label when username is unavailable (e.g. legacy session). */
const USERNAME_FALLBACK = "Kullanıcı";

interface NavChildItem {
  label: string;
  to: string;
  icon: React.ReactNode;
}

interface NavItem {
  label: string;
  to?: string;
  icon: React.ReactNode;
  children?: NavChildItem[];
}

const NAV_ITEMS: Record<UserRole, NavItem[]> = {
  ADMIN: [
    { label: "Genel Bakış", to: "/admin", icon: <LayoutDashboard className="h-4 w-4" /> },
    { label: "Bölümler", to: "/admin/departments", icon: <Settings className="h-4 w-4" /> },
    { label: "Dersler", to: "/admin/courses", icon: <BookOpen className="h-4 w-4" /> },
    {
      label: "Mezuniyet Şartları",
      icon: <GraduationCap className="h-4 w-4" />,
      children: [
        {
          label: "Mezuniyet Kategorileri",
          to: "/admin/graduation-categories",
          icon: <ListChecks className="h-4 w-4" />,
        },
        {
          label: "Muafiyet Kuralları",
          to: "/admin/exemption-rules",
          icon: <Scale className="h-4 w-4" />,
        },
      ],
    },
    { label: "Kullanıcılar", to: "/admin/users", icon: <Users className="h-4 w-4" /> },
  ],
  TEACHER: [
    { label: "Transkript Yükle", to: "/teacher", icon: <Upload className="h-4 w-4" /> },
    { label: "Analiz Geçmişi", to: "/teacher/history", icon: <GraduationCap className="h-4 w-4" /> },
  ],
  STUDENT: [
    { label: "Mezuniyet Durumu", to: "/student/results", icon: <GraduationCap className="h-4 w-4" /> },
  ],
};

interface SidebarNavProps {
  role: UserRole;
  collapsed: boolean;
}

function isNavGroupActive(children: NavChildItem[], pathname: string): boolean {
  return children.some(
    (child) => pathname === child.to || pathname.startsWith(`${child.to}/`),
  );
}

function SidebarNav({ role, collapsed }: SidebarNavProps) {
  const items = NAV_ITEMS[role] ?? [];
  const { pathname } = useLocation();
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({});

  useEffect(() => {
    items.forEach((item) => {
      if (item.children && isNavGroupActive(item.children, pathname)) {
        setOpenGroups((prev) => ({ ...prev, [item.label]: true }));
      }
    });
  }, [pathname, items]);

  function toggleGroup(label: string) {
    setOpenGroups((prev) => ({ ...prev, [label]: !prev[label] }));
  }

  const navLinkClassName = (isActive: boolean, indented = false) =>
    cn(
      "flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors duration-150",
      "hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
      isActive ? "bg-primary/10 text-primary font-medium" : "text-foreground/80",
      collapsed && "justify-center px-2",
      indented && !collapsed && "pl-9",
    );

  return (
    <nav aria-label="Ana navigasyon" className="flex flex-col gap-1 px-2">
      {items.map((item) => {
        if (item.children) {
          const isGroupActive = isNavGroupActive(item.children, pathname);
          const isOpen = openGroups[item.label] ?? false;
          const panelId = `nav-group-${item.label.replace(/\s+/g, "-").toLowerCase()}`;

          return (
            <div key={item.label} className="flex flex-col gap-0.5">
              <button
                type="button"
                onClick={() => toggleGroup(item.label)}
                aria-expanded={isOpen}
                aria-controls={panelId}
                className={cn(
                  navLinkClassName(isGroupActive),
                  "w-full cursor-pointer border-0 bg-transparent",
                  !collapsed && "justify-between",
                )}
                title={collapsed ? item.label : undefined}
              >
                <span className="flex items-center gap-3">
                  {item.icon}
                  {!collapsed && <span>{item.label}</span>}
                </span>
                {!collapsed && (
                  <ChevronDown
                    className={cn("h-4 w-4 shrink-0", isOpen && "rotate-180")}
                    aria-hidden="true"
                  />
                )}
              </button>
              {isOpen && (
                <div id={panelId} role="group" aria-label={item.label} className="flex flex-col gap-0.5">
                  {item.children.map((child) => (
                    <NavLink
                      key={child.to}
                      to={child.to}
                      className={({ isActive }) => navLinkClassName(isActive, !collapsed)}
                      title={collapsed ? child.label : undefined}
                    >
                      {child.icon}
                      {!collapsed && <span>{child.label}</span>}
                    </NavLink>
                  ))}
                </div>
              )}
            </div>
          );
        }

        return (
          <NavLink
            key={item.to}
            to={item.to!}
            end={item.to === "/admin" || item.to === "/teacher"}
            className={({ isActive }) => navLinkClassName(isActive)}
            title={collapsed ? item.label : undefined}
          >
            {item.icon}
            {!collapsed && <span>{item.label}</span>}
          </NavLink>
        );
      })}
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
  const { role, username, clearAuth } = useAuthStore();
  const navigate = useNavigate();

  function handleLogout() {
    clearAuth();
    navigate("/login", { replace: true });
  }

  function handleChangePassword() {
    navigate("/change-password");
  }

  const userRole = role ?? "STUDENT";
  const displayUsername = username ?? USERNAME_FALLBACK;
  const roleLabel =
    userRole === "ADMIN" ? "Yönetici" : userRole === "TEACHER" ? "Öğretmen" : "Öğrenci";

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
                  <Button
                    variant="ghost"
                    size="sm"
                    className="max-w-[160px] gap-2 px-2 transition-colors duration-150"
                    aria-label="Kullanıcı menüsü"
                    title={displayUsername}
                  />
                }
              >
                <User className="h-4 w-4 shrink-0" />
                <span className="truncate text-sm">{displayUsername}</span>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48 shadow-md">
                <div className="px-2 py-1.5">
                  <p className="truncate text-sm font-medium" title={displayUsername}>
                    {displayUsername}
                  </p>
                  <p className="text-xs text-muted-foreground">{roleLabel}</p>
                </div>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={handleChangePassword}
                  className="cursor-pointer transition-colors duration-150"
                >
                  <KeyRound className="mr-2 h-4 w-4" />
                  Şifre Değiştir
                </DropdownMenuItem>
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
