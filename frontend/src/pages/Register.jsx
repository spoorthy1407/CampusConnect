import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api";
import { useAuth } from "../AuthContext";
import { Err } from "../ui";

export default function Register() {
  const [f, setF] = useState({ fullName: "", email: "", password: "" });
  const [err, setErr] = useState("");
  const { login } = useAuth();
  const nav = useNavigate();
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setErr("");
    try {
      login(await api("/api/auth/register", { method: "POST", body: f }));
      nav("/");
    } catch (ex) { setErr(ex.message); }
  };

  return (
    <div className="auth-wrap">
      <form className="card auth" onSubmit={submit}>
        <h1>🏫 CampusConnect</h1>
        <h2>Student Registration</h2>
        <input placeholder="Full name" value={f.fullName} onChange={set("fullName")} required />
        <input type="email" placeholder="College email" value={f.email} onChange={set("email")} required />
        <input type="password" placeholder="Password (min 6 chars)" value={f.password} onChange={set("password")} required />
        <Err msg={err} />
        <button className="btn">Register</button>
        <p>Already registered? <Link to="/login">Login</Link></p>
      </form>
    </div>
  );
}
