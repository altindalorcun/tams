import { cn } from "@/lib/utils";
import hacettepeLogoHorizontal from "@/assets/logos/hacettepe-logo-horizontal.png";

interface HacettepeLogoHorizontalProps {
  className?: string;
}

/**
 * Hacettepe University horizontal logo (shield + wordmark).
 * Reserved for the /about page or print views.
 */
export function HacettepeLogoHorizontal({ className }: HacettepeLogoHorizontalProps) {
  return (
    <img
      src={hacettepeLogoHorizontal}
      alt="Hacettepe Üniversitesi"
      className={cn(className)}
    />
  );
}
