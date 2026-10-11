import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { isStaff } from "./ui";

export default function Navbar() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  return (
    <header className="navbar">
      <span className="brand">🏫 CampusConnect</span>
      <nav>
        <NavLink to="/" end>Dashboard</NavLink>
        <NavLink to="/book">Book a Service</NavLink>
        <NavLink to="/print">Print</NavLink>
        <NavLink to="/marketplace">Marketplace</NavLink>
        <NavLink to="/profile">Profile</NavLink>
        {isStaff(user) && <NavLink to="/admin">{user.role === "SUPER_ADMIN" ? "Super Admin" : "Admin"}</NavLink>}
      </nav>
      <span className="who">
        {user.fullName}
        <button className="btn small ghost" onClick={() => { logout(); nav("/login"); }}>Logout</button>
      </span>
    </header>
  );
}
