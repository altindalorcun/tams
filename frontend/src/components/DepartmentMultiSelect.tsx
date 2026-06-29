import { useMemo, useState } from "react";
import { Check, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Popover, PopoverContent, PopoverHeader, PopoverTitle, PopoverTrigger } from "@/components/ui/popover";
import type { Department } from "@/types";

interface DepartmentMultiSelectProps {
  departments: Department[];
  value: string[];
  onChange: (ids: string[]) => void;
  disabled?: boolean;
  isLoading?: boolean;
}

/**
 * Multi-select picker for linking a course to one or more department pools.
 */
export function DepartmentMultiSelect({
  departments,
  value,
  onChange,
  disabled = false,
  isLoading = false,
}: DepartmentMultiSelectProps) {
  const [pickerOpen, setPickerOpen] = useState(false);
  const selectedSet = useMemo(() => new Set(value), [value]);

  const departmentById = useMemo(
    () => new Map(departments.map((d) => [d.id, d])),
    [departments],
  );

  function handleToggle(departmentId: string) {
    if (selectedSet.has(departmentId)) {
      onChange(value.filter((id) => id !== departmentId));
    } else {
      onChange([...value, departmentId]);
    }
  }

  function handleRemove(departmentId: string) {
    onChange(value.filter((id) => id !== departmentId));
  }

  const triggerLabel = value.length === 0 ? "Bölüm seçilmedi" : "Bölüm seç";

  if (isLoading) {
    return <Skeleton className="h-9 w-full" />;
  }

  return (
    <div className="space-y-2">
      <Popover open={pickerOpen} onOpenChange={setPickerOpen}>
        <PopoverTrigger
          render={
            <Button
              type="button"
              variant="outline"
              disabled={disabled}
              className={`w-full justify-start font-normal ${value.length === 0 ? "text-muted-foreground" : ""}`}
            >
              {triggerLabel}
              {value.length > 0 && (
                <Badge variant="secondary" className="ml-auto">{value.length} seçili</Badge>
              )}
            </Button>
          }
        />
        <PopoverContent align="start" className="w-80 shadow-md p-0">
          <PopoverHeader className="px-4 pt-4">
            <PopoverTitle>Bölüm Seç</PopoverTitle>
          </PopoverHeader>
          <div className="flex flex-col gap-3 px-4 pb-4">
            <div className="max-h-52 overflow-y-auto rounded-md border divide-y">
              {departments.length === 0 && (
                <p className="text-sm text-muted-foreground text-center py-4 px-2">
                  Henüz bölüm tanımlanmamış.
                </p>
              )}
              {departments.map((department) => {
                const selected = selectedSet.has(department.id);
                return (
                  <button
                    key={department.id}
                    type="button"
                    className={`flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-muted/50 transition-colors duration-150 ${selected ? "bg-muted/50" : ""}`}
                    onClick={() => handleToggle(department.id)}
                  >
                    {selected ? (
                      <Check className="h-3.5 w-3.5 shrink-0 text-primary" />
                    ) : (
                      <span className="w-3.5 shrink-0" />
                    )}
                    <span className="truncate">{department.name}</span>
                  </button>
                );
              })}
            </div>
          </div>
        </PopoverContent>
      </Popover>
      {value.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {value.map((departmentId) => {
            const department = departmentById.get(departmentId);
            return (
              <Badge key={departmentId} variant="outline" className="text-xs gap-1 pr-1">
                {department?.name ?? departmentId}
                <button
                  type="button"
                  className="rounded-sm hover:bg-muted p-0.5"
                  aria-label={`${department?.name ?? departmentId} bölümünü kaldır`}
                  onClick={() => handleRemove(departmentId)}
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            );
          })}
        </div>
      )}
    </div>
  );
}
