import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Route, Routes } from "react-router";
import { Layout } from "./components/Layout.tsx";
import { HomePage } from "./pages/HomePage.tsx";
import { SignInPage } from "./pages/SignInPage.tsx";
import { SignUpPage } from "./pages/SignUpPage.tsx";
import { TeamPage } from "./pages/TeamPage.tsx";
import { ProfilePage } from "./pages/ProfilePage.tsx";
import { SessionProvider } from "./session.tsx";
import "./styles.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <SessionProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="team" element={<TeamPage />} />
            <Route path="login" element={<SignInPage />} />
            <Route path="signup" element={<SignUpPage />} />
            <Route path=":handle" element={<ProfilePage tab="grunts" />} />
            <Route path=":handle/following" element={<ProfilePage tab="following" />} />
            <Route path=":handle/followers" element={<ProfilePage tab="followers" />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </SessionProvider>
  </StrictMode>,
);
