import { cn } from "@/lib/utils";
import logoIcon from "@/assets/logos/tams-main-logo.png";

interface LogoIconProps {
  className?: string;
}

/**
 * TAMS icon-only brand mark (graduation cap without text).
 * Use in the AppShell sidebar / header top-left.
 */
export function LogoIcon({ className }: LogoIconProps) {
  return (
    <img
      src={logoIcon}
      alt="TAMS"
      className={cn("h-8 w-auto", className)}
    />
  );
}
