import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

import type { CurrentUser } from "@/lib/api/types";

type AuthStatus = "checking" | "authenticated" | "unauthenticated";

interface AuthState {
  user: CurrentUser | null;
  status: AuthStatus;
}

const initialState: AuthState = {
  user: null,
  status: "checking",
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    authenticated(state, action: PayloadAction<CurrentUser>) {
      state.user = action.payload;
      state.status = "authenticated";
    },
    unauthenticated(state) {
      state.user = null;
      state.status = "unauthenticated";
    },
    checkingSession(state) {
      state.status = "checking";
    },
  },
});

export const { authenticated, unauthenticated, checkingSession } =
  authSlice.actions;
export default authSlice.reducer;
