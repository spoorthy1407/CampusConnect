import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api";
import { useAuth } from "../AuthContext";
import { Err } from "../ui";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [err, setErr] = useState("");
  const { login } = useAuth();
  const nav = useNavigate();

  const submit = async (e) => {
    e.preventDefault();
    setErr("");
    try {
      login(await api("/api/auth/login", { method: "POST", body: { email, password } }));
      nav("/");
    } catch (ex) { setErr(ex.message); }
  };

  return (
    <div className="auth-wrap">
      <form className="card auth" onSubmit={submit}>
        <h1>🏫 CampusConnect</h1>
        <h2>Login</h2>
        <input type="email" placeholder="Enter email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <input type="password" placeholder="Enter password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        <Err msg={err} />
        <button className="btn">Login</button>
        <p>New here? <Link to="/register">Create an account</Link></p>
      </form>
    </div>
  );
}
