import { Navigate, Route, Routes, Outlet } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { isStaff } from "./ui";
import Navbar from "./Navbar";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Booking from "./pages/Booking";
import Print from "./pages/Print";
import Marketplace from "./pages/Marketplace";
import Profile from "./pages/Profile";
import Admin from "./pages/Admin";

function Protected({ admin }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (admin && !isStaff(user)) return <Navigate to="/" replace />;
  return (
    <>
      <Navbar />
      <main className="container"><Outlet /></main>
    </>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route element={<Protected />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/book" element={<Booking />} />
        <Route path="/print" element={<Print />} />
        <Route path="/marketplace" element={<Marketplace />} />
        <Route path="/profile" element={<Profile />} />
      </Route>
      <Route element={<Protected admin />}>
        <Route path="/admin" element={<Admin />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
