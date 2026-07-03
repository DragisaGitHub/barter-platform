import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  activateAdminCategorySchema,
  createAdminCategorySchema,
  createAdminCategorySchemaField,
  createAdminCategorySchemaFieldOption,
  deleteAdminCategorySchema,
  deleteAdminCategorySchemaField,
  deleteAdminCategorySchemaFieldOption,
  getAdminCategorySchema,
  listAdminCategorySchemas,
  updateAdminCategorySchema,
  updateAdminCategorySchemaField,
  updateAdminCategorySchemaFieldOption,
  type ListAdminCategorySchemasParams,
} from "@/api/adminCategorySchemasApi.ts";
import type {
  CategorySchemaFieldResponse,
  CategorySchemaPagedResponse,
  CategorySchemaResponse,
  CreateCategorySchemaFieldRequest,
  CreateCategorySchemaRequest,
  CreateFieldOptionRequest,
  FieldOptionResponse,
  UpdateCategorySchemaFieldRequest,
  UpdateCategorySchemaRequest,
  UpdateFieldOptionRequest,
} from "@/api/generated/types.ts";

export const adminCategorySchemaKeys = {
  all: ["admin", "categorySchemas"] as const,
  lists: () => ["admin", "categorySchemas", "list"] as const,
  list: (params: ListAdminCategorySchemasParams) => ["admin", "categorySchemas", "list", params] as const,
  details: () => ["admin", "categorySchemas", "detail"] as const,
  detail: (schemaUuid: string) => ["admin", "categorySchemas", "detail", schemaUuid] as const,
};

export function useAdminCategorySchemas(params: ListAdminCategorySchemasParams = {}, enabled = true) {
  return useQuery<CategorySchemaPagedResponse>({
    queryKey: adminCategorySchemaKeys.list(params),
    queryFn: () => listAdminCategorySchemas(params),
    enabled,
    placeholderData: keepPreviousData,
  });
}

export function useAdminCategorySchema(schemaUuid: string, enabled = true) {
  return useQuery<CategorySchemaResponse>({
    queryKey: adminCategorySchemaKeys.detail(schemaUuid),
    queryFn: () => getAdminCategorySchema(schemaUuid),
    enabled: enabled && !!schemaUuid,
  });
}

export function useCreateAdminCategorySchema() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ categoryUuid, data }: { categoryUuid: string; data: CreateCategorySchemaRequest }) =>
      createAdminCategorySchema(categoryUuid, data),
    onSuccess: (schema) => {
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.all });
      queryClient.setQueryData(adminCategorySchemaKeys.detail(schema.uuid), schema);
    },
  });
}

export function useUpdateAdminCategorySchema() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ schemaUuid, data }: { schemaUuid: string; data: UpdateCategorySchemaRequest }) =>
      updateAdminCategorySchema(schemaUuid, data),
    onSuccess: (schema) => {
      queryClient.setQueryData(adminCategorySchemaKeys.detail(schema.uuid), schema);
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.all });
    },
  });
}

export function useDeleteAdminCategorySchema() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (schemaUuid: string) => deleteAdminCategorySchema(schemaUuid),
    onSuccess: (_, schemaUuid) => {
      queryClient.removeQueries({ queryKey: adminCategorySchemaKeys.detail(schemaUuid) });
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.all });
    },
  });
}

export function useActivateAdminCategorySchema() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (schemaUuid: string) => activateAdminCategorySchema(schemaUuid),
    onSuccess: (schema) => {
      queryClient.setQueryData(adminCategorySchemaKeys.detail(schema.uuid), schema);
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.all });
    },
  });
}

export function useCreateAdminCategorySchemaField() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ schemaUuid, data }: { schemaUuid: string; data: CreateCategorySchemaFieldRequest }) =>
      createAdminCategorySchemaField(schemaUuid, data),
    onSuccess: (_field: CategorySchemaFieldResponse, variables) => {
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.detail(variables.schemaUuid) });
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.all });
    },
  });
}

export function useUpdateAdminCategorySchemaField() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (variables: {
      schemaUuid: string;
      fieldUuid: string;
      data: UpdateCategorySchemaFieldRequest;
    }) => updateAdminCategorySchemaField(variables.fieldUuid, variables.data),
    onSuccess: (_field: CategorySchemaFieldResponse, variables) => {
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.detail(variables.schemaUuid) });
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.all });
    },
  });
}

export function useDeleteAdminCategorySchemaField() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (variables: { schemaUuid: string; fieldUuid: string }) =>
      deleteAdminCategorySchemaField(variables.fieldUuid),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.detail(variables.schemaUuid) });
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.all });
    },
  });
}

export function useCreateAdminCategorySchemaFieldOption() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (variables: {
      schemaUuid: string;
      fieldUuid: string;
      data: CreateFieldOptionRequest;
    }) => createAdminCategorySchemaFieldOption(variables.fieldUuid, variables.data),
    onSuccess: (_option: FieldOptionResponse, variables) => {
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.detail(variables.schemaUuid) });
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.all });
    },
  });
}

export function useUpdateAdminCategorySchemaFieldOption() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (variables: {
      schemaUuid: string;
      optionUuid: string;
      data: UpdateFieldOptionRequest;
    }) => updateAdminCategorySchemaFieldOption(variables.optionUuid, variables.data),
    onSuccess: (_option: FieldOptionResponse, variables) => {
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.detail(variables.schemaUuid) });
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.all });
    },
  });
}

export function useDeleteAdminCategorySchemaFieldOption() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (variables: { schemaUuid: string; optionUuid: string }) =>
      deleteAdminCategorySchemaFieldOption(variables.optionUuid),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.detail(variables.schemaUuid) });
      queryClient.invalidateQueries({ queryKey: adminCategorySchemaKeys.all });
    },
  });
}

