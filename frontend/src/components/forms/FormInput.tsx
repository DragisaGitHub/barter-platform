import { type InputHTMLAttributes } from "react";
import { useFormContext } from "react-hook-form";
import { Input } from "../ui/Input";

interface FormInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "name"> {
  name: string;
  label?: string;
}

export function FormInput({ name, label, ...props }: FormInputProps) {
  const {
    register,
    formState: { errors },
  } = useFormContext();

  const error = errors[name]?.message as string | undefined;

  return <Input {...register(name)} label={label} error={error} {...props} />;
}
