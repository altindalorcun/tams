import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

/**
 * Password input with a toggle to show or hide the entered value.
 * Visibility defaults to hidden (type="password").
 */
export function PasswordInput({
  className,
  ...props
}: React.ComponentProps<typeof Input>) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="relative">
      <Input
        type={visible ? "text" : "password"}
        className={cn("pr-10", className)}
        {...props}
      />
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        className="absolute right-0 top-0 h-full rounded-l-none transition-colors duration-150"
        onClick={() => setVisible((prev) => !prev)}
        aria-label={visible ? "Şifreyi gizle" : "Şifreyi göster"}
      >
        {visible ? <EyeOff /> : <Eye />}
      </Button>
    </div>
  );
}
