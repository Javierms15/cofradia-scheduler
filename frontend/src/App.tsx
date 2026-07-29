import { Route, Routes } from "react-router-dom";
import { RequireAuth } from "./components/RequireAuth";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { EscenariosPage } from "./pages/EscenariosPage";
import { EscenarioDetailPage } from "./pages/EscenarioDetailPage";
import { CofradiaDetailPage } from "./pages/CofradiaDetailPage";
import { ResultadoPage } from "./pages/ResultadoPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <EscenariosPage />
          </RequireAuth>
        }
      />
      <Route
        path="/escenarios/:id"
        element={
          <RequireAuth>
            <EscenarioDetailPage />
          </RequireAuth>
        }
      />
      <Route
        path="/cofradias/:id"
        element={
          <RequireAuth>
            <CofradiaDetailPage />
          </RequireAuth>
        }
      />
      <Route
        path="/resultados/:id"
        element={
          <RequireAuth>
            <ResultadoPage />
          </RequireAuth>
        }
      />
    </Routes>
  );
}
