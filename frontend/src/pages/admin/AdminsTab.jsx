import { useCallback, useEffect, useState } from "react";
import { api } from "../../api";
import { Err, Flash } from "../../ui";

function FacilityChecks({ facilities, ids, onChange }) {
  const toggle = (id) => onChange(ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id]);
  return (
    <div className="checks">
      {facilities.map((f) => (
        <label key={f.id} className="check">
          <input type="checkbox" checked={ids.includes(f.id)} onChange={() => toggle(f.id)} /> {f.name}
        </label>
      ))}
    </div>
  );
}

function AdminRow({ admin, facilities, onSave, onRemove }) {
  const [ids, setIds] = useState(admin.facilityIds);
  return (
    <tr>
      <td>{admin.fullName}<br /><small className="muted">{admin.email}</small></td>
      <td><FacilityChecks facilities={facilities} ids={ids} onChange={setIds} /></td>
      <td className="actions">
        <button className="btn small" disabled={!ids.length} onClick={() => onSave(admin.id, ids)}>Save access</button>
        <button className="btn small ghost" onClick={() => onRemove(admin)}>Remove admin</button>
      </td>
    </tr>
  );
}

export default function AdminsTab({ facilities }) {
  const [admins, setAdmins] = useState([]);
  const [form, setForm] = useState({ fullName: "", email: "", password: "", facilityIds: [] });
  const [err, setErr] = useState("");
  const [ok, setOk] = useState("");

  const load = useCallback(() => {
    api("/api/super/admins").then(setAdmins).catch((e) => setErr(e.message));
  }, []);
  useEffect(() => { load(); }, [load]);

  const run = async (fn, message) => {
    setErr("");
    setOk("");
    try { await fn(); setOk(message); load(); return true; } catch (e) { setErr(e.message); return false; }
  };

  const create = async (e) => {
    e.preventDefault();
    const done = await run(() => api("/api/super/admins", { method: "POST", body: form }), "Admin saved.");
    if (done) setForm({ fullName: "", email: "", password: "", facilityIds: [] });
  };

  const save = (id, ids) =>
    run(() => api(`/api/super/admins/${id}/facilities`, { method: "PUT", body: { facilityIds: ids } }), "Access updated.");

  const remove = (a) => {
    if (!window.confirm(`Remove admin access for ${a.fullName}? They will become a normal student.`)) return;
    run(() => api(`/api/super/admins/${a.id}`, { method: "DELETE" }), "Admin removed.");
  };

  return (
    <>
      <Err msg={err} />
      <Flash msg={ok} />
      <form className="card form wide" onSubmit={create}>
        <h3>Add a facility admin</h3>
        <p className="muted">Each admin can only see and approve bookings and print jobs of the facilities ticked below. If the email already belongs to a student, that student is promoted and the password is ignored.</p>
        <div className="row3">
          <label>Full name<input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} /></label>
          <label>Email<input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></label>
          <label>Password (new accounts)<input type="password" minLength={6} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></label>
        </div>
        <FacilityChecks facilities={facilities} ids={form.facilityIds} onChange={(ids) => setForm({ ...form, facilityIds: ids })} />
        <button className="btn" disabled={!form.facilityIds.length}>Save admin</button>
      </form>

      <table className="card">
        <thead><tr><th>Admin</th><th>Facilities they manage</th><th></th></tr></thead>
        <tbody>
          {admins.map((a) => (
            <AdminRow key={`${a.id}:${a.facilityIds.join(",")}`} admin={a} facilities={facilities} onSave={save} onRemove={remove} />
          ))}
          {!admins.length && <tr><td colSpan="3" className="muted">No facility admins yet. Add one above.</td></tr>}
        </tbody>
      </table>
    </>
  );
}
