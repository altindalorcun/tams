import { cn } from "@/lib/utils";
import logoFull from "@/assets/logos/tams-main-logo-with-words.png";

interface LogoFullProps {
  className?: string;
}

/**
 * Full TAMS brand mark with graduation cap, wordmark, and subtitle.
 * Use only on the /login page, centered.
 */
export function LogoFull({ className }: LogoFullProps) {
  return (
    <img
      src={logoFull}
      alt="TAMS — Transkript Analiz ve Mezuniyet Sistemi"
      className={cn("max-w-[280px] w-full", className)}
    />
  );
}
