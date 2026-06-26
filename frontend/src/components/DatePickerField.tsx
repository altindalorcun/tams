import { useState } from "react";
import { CalendarIcon } from "lucide-react";
import { tr } from "react-day-picker/locale/tr";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import { formatDateDdMmYyyy, normalizeSelectedDate } from "@/lib/dateFilter";

interface DatePickerFieldProps {
  id?: string;
  value: Date | null;
  onChange: (date: Date | null) => void;
  placeholder?: string;
  className?: string;
}

/**
 * Single-date picker that displays the selected day as dd/mm/yyyy.
 */
export function DatePickerField({
  id,
  value,
  onChange,
  placeholder = "Tarih seçin",
  className,
}: DatePickerFieldProps) {
  const [open, setOpen] = useState(false);

  function handleSelect(date: Date | undefined) {
    onChange(normalizeSelectedDate(date));
    setOpen(false);
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        id={id}
        render={
          <Button
            variant="outline"
            className={cn(
              "w-full justify-start font-normal transition-colors duration-150",
              !value && "text-muted-foreground",
              className,
            )}
          />
        }
      >
        <CalendarIcon className="mr-2 h-4 w-4" />
        {value ? formatDateDdMmYyyy(value) : placeholder}
      </PopoverTrigger>
      <PopoverContent align="start" className="w-auto p-0 shadow-md">
        <Calendar
          mode="single"
          selected={value ?? undefined}
          onSelect={handleSelect}
          locale={tr}
          defaultMonth={value ?? undefined}
        />
      </PopoverContent>
    </Popover>
  );
}
