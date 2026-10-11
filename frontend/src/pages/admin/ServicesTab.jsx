import { useCallback, useEffect, useState } from "react";
import { api } from "../../api";
import { Badge, Err } from "../../ui";
import FacilityFilter from "./FacilityFilter";

const SUGGESTIONS = ["Chamber", "Computer", "Table", "Study Room", "Group Table", "Badminton", "Basketball", "Tutoring"];

export default function ServicesTab({ facilities }) {
  const [rows, setRows] = useState([]);
  const [fid, setFid] = useState("");
  const [form, setForm] = useState({ facilityId: "", name: "", subType: "", capacity: 1, location: "" });
  const [err, setErr] = useState("");

  const load = useCallback(() => {
    api("/api/admin/services").then(setRows).catch((e) => setErr(e.message));
  }, []);
  useEffect(() => { load(); }, [load]);

  const act = async (path, method, body) => {
    setErr("");
    try { await api(path, { method, body }); load(); return true; } catch (e) { setErr(e.message); return false; }
  };

  const add = async (e) => {
    e.preventDefault();
    const ok = await act("/api/admin/services", "POST", {
      ...form, facilityId: Number(form.facilityId), capacity: Number(form.capacity) || 1,
    });
    if (ok) setForm({ ...form, name: "", location: "" });
  };

  const toggle = (s) =>
    act(`/api/admin/services/${s.id}`, "PUT", {
      name: s.name, facilityId: s.facility.id, subType: s.subType, capacity: s.capacity, location: s.location,
      status: s.status === "AVAILABLE" ? "MAINTENANCE" : "AVAILABLE",
    });

  const list = rows.filter((s) => s.type !== "PRINTING" && (!fid || String(s.facility.id) === fid));
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  return (
    <>
      <Err msg={err} />
      <form className="card form inline" onSubmit={add}>
        <select value={form.facilityId} onChange={set("facilityId")} required>
          <option value="">Facility…</option>
          {facilities.map((f) => <option key={f.id} value={f.id}>{f.name}</option>)}
        </select>
        <input placeholder="Name (e.g. Computer 09)" value={form.name} onChange={set("name")} required />
        <input list="subtypes" placeholder="Kind (Chamber, Computer…)" value={form.subType} onChange={set("subType")} />
        <datalist id="subtypes">{SUGGESTIONS.map((s) => <option key={s} value={s} />)}</datalist>
        <input type="number" min="1" placeholder="Seats" value={form.capacity} onChange={set("capacity")} />
        <input placeholder="Location / room" value={form.location} onChange={set("location")} />
        <button className="btn">Add resource</button>
      </form>

      <FacilityFilter facilities={facilities} value={fid} onChange={setFid} />
      <table className="card">
        <thead><tr><th>Facility</th><th>Name</th><th>Kind</th><th>Seats</th><th>Location</th><th>Status</th><th></th></tr></thead>
        <tbody>
          {list.map((s) => (
            <tr key={s.id}>
              <td>{s.facility.name}</td><td>{s.name}</td><td>{s.subType}</td><td>{s.capacity}</td><td>{s.location}</td>
              <td><Badge s={s.status} /></td>
              <td><button className="btn small ghost" onClick={() => toggle(s)}>{s.status === "AVAILABLE" ? "Set maintenance" : "Set available"}</button></td>
            </tr>
          ))}
          {!list.length && <tr><td colSpan="7" className="muted">No resources yet.</td></tr>}
        </tbody>
      </table>
    </>
  );
}
