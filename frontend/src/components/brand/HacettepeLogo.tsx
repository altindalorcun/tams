import { cn } from "@/lib/utils";
import hacettepeLogo from "@/assets/logos/hacettepe-logo.png";

interface HacettepeLogoProps {
  className?: string;
}

/**
 * Hacettepe University shield emblem.
 * Use in the /login page bottom-left corner at reduced opacity.
 */
export function HacettepeLogo({ className }: HacettepeLogoProps) {
  return (
    <img
      src={hacettepeLogo}
      alt="Hacettepe Üniversitesi"
      className={cn("max-w-[100px] opacity-60", className)}
    />
  );
}
