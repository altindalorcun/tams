import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Check, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Popover, PopoverContent, PopoverHeader, PopoverTitle, PopoverTrigger } from "@/components/ui/popover";
import { getDepartmentCourses } from "@/api/ruleApi";
import { matchesTextFilter } from "@/lib/textFilter";
import type { DepartmentCourse } from "@/types";

interface DepartmentCoursePickerBaseProps {
  departmentId: string;
  enabled: boolean;
  triggerLabel: string;
}

interface DepartmentCoursePickerMultipleProps extends DepartmentCoursePickerBaseProps {
  mode: "multiple";
  value: string[];
  onChange: (codes: string[]) => void;
}

interface DepartmentCoursePickerSingleProps extends DepartmentCoursePickerBaseProps {
  mode: "single";
  value: string;
  onChange: (code: string) => void;
}

export type DepartmentCoursePickerProps =
  | DepartmentCoursePickerMultipleProps
  | DepartmentCoursePickerSingleProps;

/**
 * Picker for course codes from a department's course pool.
 * Supports multi-select (toggle) or single-select (pick one and close).
 */
export function DepartmentCoursePicker(props: DepartmentCoursePickerProps) {
  const { departmentId, enabled, triggerLabel, mode } = props;
  const [codeFilter, setCodeFilter] = useState("");
  const [nameFilter, setNameFilter] = useState("");
  const [pickerOpen, setPickerOpen] = useState(false);

  const { data: courses = [], isLoading } = useQuery({
    queryKey: ["department-courses", departmentId],
    queryFn: () => getDepartmentCourses(departmentId),
    enabled,
  });

  const filteredCourses = useMemo(() => {
    return courses.filter((c: DepartmentCourse) => {
      if (!matchesTextFilter(c.courseCode, codeFilter)) return false;
      if (!matchesTextFilter(c.courseName, nameFilter)) return false;
      return true;
    });
  }, [courses, codeFilter, nameFilter]);

  const multipleValue = mode === "multiple" ? props.value : [];
  const singleValue = mode === "single" ? props.value : "";
  const selectedSet = useMemo(() => new Set(multipleValue), [multipleValue]);

  function handlePickerOpenChange(open: boolean) {
    if (!open) {
      setCodeFilter("");
      setNameFilter("");
    }
    setPickerOpen(open);
  }

  function handleMultipleToggle(code: string) {
    if (mode !== "multiple") return;
    const upper = code.toUpperCase();
    if (selectedSet.has(upper)) {
      props.onChange(multipleValue.filter((c) => c !== upper));
    } else {
      props.onChange([...multipleValue, upper]);
    }
  }

  function handleSingleSelect(code: string) {
    if (mode !== "single") return;
    props.onChange(code.toUpperCase());
    setPickerOpen(false);
  }

  function handleMultipleRemove(code: string) {
    if (mode !== "multiple") return;
    props.onChange(multipleValue.filter((c) => c !== code));
  }

  function handleSingleClear() {
    if (mode !== "single") return;
    props.onChange("");
    setPickerOpen(false);
  }

  function handleRowClick(code: string) {
    if (mode === "multiple") {
      handleMultipleToggle(code);
    } else {
      handleSingleSelect(code);
    }
  }

  function isRowSelected(code: string): boolean {
    const upper = code.toUpperCase();
    if (mode === "multiple") return selectedSet.has(upper);
    return singleValue.toUpperCase() === upper;
  }

  const triggerText =
    mode === "single" && singleValue
      ? singleValue
      : triggerLabel;

  return (
    <div className="space-y-2">
      <Popover open={pickerOpen} onOpenChange={handlePickerOpenChange}>
        <PopoverTrigger
          render={
            <Button
              type="button"
              variant="outline"
              className={`w-full justify-start font-normal ${mode === "single" && !singleValue ? "text-muted-foreground" : ""}`}
            >
              {triggerText}
              {mode === "multiple" && multipleValue.length > 0 && (
                <Badge variant="secondary" className="ml-auto">{multipleValue.length} seçili</Badge>
              )}
            </Button>
          }
        />
        <PopoverContent align="start" className="w-96 shadow-md p-0">
          <PopoverHeader className="px-4 pt-4">
            <PopoverTitle>Bölüm Ders Havuzundan Seç</PopoverTitle>
          </PopoverHeader>
          <div className="flex flex-col gap-3 px-4 pb-4">
            <Input
              className="font-mono"
              placeholder="Ders koduna göre filtrele"
              value={codeFilter}
              onChange={(e) => setCodeFilter(e.target.value)}
            />
            <Input
              placeholder="Ders adına göre filtrele"
              value={nameFilter}
              onChange={(e) => setNameFilter(e.target.value)}
            />
            <div className="max-h-52 overflow-y-auto rounded-md border divide-y">
              {isLoading && <p className="text-sm text-muted-foreground text-center py-4">Yükleniyor…</p>}
              {!isLoading && courses.length === 0 && (
                <p className="text-sm text-muted-foreground text-center py-4 px-2">
                  Bu bölüme henüz ders eklenmemiş. Önce Bölümler sekmesinden ders havuzunu doldurun.
                </p>
              )}
              {!isLoading && courses.length > 0 && filteredCourses.length === 0 && (
                <p className="text-sm text-muted-foreground text-center py-4">Filtreye uygun ders bulunamadı.</p>
              )}
              {filteredCourses.map((c: DepartmentCourse) => {
                const selected = isRowSelected(c.courseCode);
                return (
                  <button
                    key={c.courseId}
                    type="button"
                    className={`flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-muted/50 transition-colors duration-150 ${selected ? "bg-muted/50" : ""}`}
                    onClick={() => handleRowClick(c.courseCode)}
                  >
                    {selected ? <Check className="h-3.5 w-3.5 shrink-0 text-primary" /> : <span className="w-3.5 shrink-0" />}
                    <span className="font-mono text-xs text-muted-foreground">{c.courseCode}</span>
                    <span className="truncate">{c.courseName}</span>
                  </button>
                );
              })}
            </div>
            {mode === "single" && singleValue && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="w-full transition-colors duration-150"
                onClick={handleSingleClear}
              >
                Temizle
              </Button>
            )}
          </div>
        </PopoverContent>
      </Popover>
      {mode === "multiple" && multipleValue.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {multipleValue.map((code) => (
            <Badge key={code} variant="outline" className="font-mono text-xs gap-1 pr-1">
              {code}
              <button
                type="button"
                className="rounded-sm hover:bg-muted p-0.5"
                aria-label={`${code} kodunu kaldır`}
                onClick={() => handleMultipleRemove(code)}
              >
                <X className="h-3 w-3" />
              </button>
            </Badge>
          ))}
        </div>
      )}
    </div>
  );
}
