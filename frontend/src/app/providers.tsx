"use client";

import { useEffect, useState, type ReactNode } from "react";
import { QueryClient, QueryClientProvider, useQuery } from "@tanstack/react-query";
import { Provider } from "react-redux";

import { getCurrentUser } from "@/lib/api/auth";
import { setUnauthorizedHandler } from "@/lib/api/client";
import { authenticated, unauthenticated } from "@/store/auth-slice";
import { useAppDispatch } from "@/store/hooks";
import { store } from "@/store";

function AuthBootstrap() {
  const dispatch = useAppDispatch();

  useEffect(
    () => setUnauthorizedHandler(() => dispatch(unauthenticated())),
    [dispatch],
  );

  const session = useQuery({
    queryKey: ["auth", "me"],
    queryFn: getCurrentUser,
    retry: false,
    staleTime: 60_000,
  });

  useEffect(() => {
    if (session.data) dispatch(authenticated(session.data));
    if (session.error) {
      dispatch(unauthenticated());
    }
  }, [dispatch, session.data, session.error]);

  return null;
}

export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { refetchOnWindowFocus: false, retry: 1 },
          mutations: { retry: false },
        },
      }),
  );

  return (
    <Provider store={store}>
      <QueryClientProvider client={queryClient}>
        <AuthBootstrap />
        {children}
      </QueryClientProvider>
    </Provider>
  );
}
