"use client";

import { apiRequest } from "@/lib/api/client";
import type {
  Batch,
  CommandResult,
  MachineEvent,
  MachineStatus,
  PageResult,
  Recipe,
  Settings,
  Telemetry,
} from "@/lib/api/types";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

const SELECTED_RECIPE = "ctm.selectedRecipeId";

export function useSettings() {
  return useQuery({
    queryKey: ["settings"],
    queryFn: () => apiRequest<Settings>("/api/v1/settings"),
  });
}

export function useMachineStatus(refreshMs = 2000) {
  return useQuery({
    queryKey: ["machine-status"],
    queryFn: () => apiRequest<MachineStatus>("/api/v1/machine/status"),
    refetchInterval: refreshMs,
  });
}

export function useMachineTelemetry(refreshMs = 2000) {
  return useQuery({
    queryKey: ["machine-telemetry"],
    queryFn: () => apiRequest<Telemetry>("/api/v1/machine/telemetry"),
    refetchInterval: refreshMs,
  });
}

export function useEvents(refreshMs = 2000) {
  return useQuery({
    queryKey: ["events"],
    queryFn: () => apiRequest<MachineEvent[]>("/api/v1/events"),
    refetchInterval: refreshMs,
  });
}

export function useRecipes(query: string) {
  return useQuery({
    queryKey: ["recipes", query],
    queryFn: () => apiRequest<Recipe[]>(`/api/v1/recipes${query ? `?q=${encodeURIComponent(query)}` : ""}`),
  });
}

export function useRecipe(id: string) {
  return useQuery({
    queryKey: ["recipe", id],
    queryFn: () => apiRequest<Recipe>(`/api/v1/recipes/${id}`),
    enabled: Boolean(id),
  });
}

export function useBatches(filters: { state?: string; from?: string; to?: string; page?: number }) {
  const params = new URLSearchParams();
  if (filters.state) params.set("state", filters.state);
  if (filters.from) params.set("from", filters.from);
  if (filters.to) params.set("to", filters.to);
  params.set("page", String(filters.page ?? 0));
  params.set("size", "20");
  return useQuery({
    queryKey: ["batches", params.toString()],
    queryFn: () => apiRequest<PageResult<Batch>>(`/api/v1/batches?${params.toString()}`),
  });
}

export function useBatch(id: string) {
  return useQuery({
    queryKey: ["batch", id],
    queryFn: () => apiRequest<Batch>(`/api/v1/batches/${id}`),
    enabled: Boolean(id),
  });
}

export function useRefreshMachine() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: () => apiRequest<MachineStatus>("/api/v1/machine/refresh", { method: "POST" }),
    onSuccess: async () => {
      await client.invalidateQueries();
    },
  });
}

export function useCommand(path: string) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (body: unknown) =>
      apiRequest<CommandResult>(path, { method: "POST", body: JSON.stringify(body) }),
    onSuccess: async () => {
      await client.invalidateQueries();
    },
  });
}

export function selectedRecipeId(): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.localStorage.getItem(SELECTED_RECIPE);
}

export function selectRecipe(id: string | null) {
  if (typeof window === "undefined") {
    return;
  }
  if (id) {
    window.localStorage.setItem(SELECTED_RECIPE, id);
  } else {
    window.localStorage.removeItem(SELECTED_RECIPE);
  }
  window.dispatchEvent(new Event("ctm-recipe"));
}
