import { useState } from "react";
import { api } from "../../api";
import { Badge, Err, TYPE_META, TYPE_ORDER, hhmm, label } from "../../ui";

const EMPTY = {
  id: null, name: "", type: "LIBRARY", blockName: "", description: "",
  openTime: "09:00", closeTime: "17:00", maxBookingMinutes: 120, active: true,
};

export default function FacilitiesTab({ facilities, onChanged }) {
  const [f, setF] = useState(EMPTY);
  const [err, setErr] = useState("");
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const save = async (e) => {
    e.preventDefault();
    setErr("");
    const body = {
      name: f.name, type: f.type, blockName: f.blockName, description: f.description,
      openTime: f.openTime, closeTime: f.closeTime,
      maxBookingMinutes: Number(f.maxBookingMinutes) || 120, active: f.active,
    };
    try {
      if (f.id) await api(`/api/super/facilities/${f.id}`, { method: "PUT", body });
      else await api("/api/super/facilities", { method: "POST", body });
      setF(EMPTY);
      onChanged();
    } catch (ex) { setErr(ex.message); }
  };

  const edit = (x) => {
    setErr("");
    setF({
      id: x.id, name: x.name, type: x.type, blockName: x.blockName || "", description: x.description || "",
      openTime: hhmm(x.openTime), closeTime: hhmm(x.closeTime), maxBookingMinutes: x.maxBookingMinutes || 120, active: x.active,
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <>
      <Err msg={err} />
      <form className="card form wide" onSubmit={save}>
        <h3>{f.id ? `Edit ${f.name}` : "Add a facility (library, space, printer, court…)"}</h3>
        <div className="row3">
          <label>Name<input value={f.name} onChange={set("name")} placeholder="e.g. D-Block Library" required /></label>
          <label>Type
            <select value={f.type} onChange={set("type")} disabled={!!f.id}>
              {TYPE_ORDER.map((t) => <option key={t} value={t}>{TYPE_META[t].label}</option>)}
            </select>
          </label>
          <label>Block / building<input value={f.blockName} onChange={set("blockName")} placeholder="e.g. D-Block" /></label>
        </div>
        <label>Details shown to students<input value={f.description} onChange={set("description")} maxLength={480} /></label>
        <div className="row3">
          <label>Opens<input type="time" value={f.openTime} onChange={set("openTime")} required /></label>
          <label>Closes<input type="time" value={f.closeTime} onChange={set("closeTime")} required /></label>
          <label>Max booking (minutes)<input type="number" min="15" step="15" value={f.maxBookingMinutes} onChange={set("maxBookingMinutes")} /></label>
        </div>
        <label className="check"><input type="checkbox" checked={f.active} onChange={(e) => setF({ ...f, active: e.target.checked })} /> Open for bookings / print jobs</label>
        <div className="row">
          <button className="btn">{f.id ? "Save changes" : "Add facility"}</button>
          {f.id && <button type="button" className="btn ghost" onClick={() => setF(EMPTY)}>Cancel edit</button>}
        </div>
      </form>

      <table className="card">
        <thead><tr><th>Name</th><th>Type</th><th>Block</th><th>Hours</th><th>Max</th><th>Status</th><th></th></tr></thead>
        <tbody>
          {facilities.map((x) => (
            <tr key={x.id}>
              <td>{x.name}</td><td>{label(x.type)}</td><td>{x.blockName}</td>
              <td>{hhmm(x.openTime)} – {hhmm(x.closeTime)}</td><td>{x.type === "PRINTING" ? "-" : `${x.maxBookingMinutes} min`}</td>
              <td><Badge s={x.active ? "ACTIVE" : "CLOSED"} /></td>
              <td><button className="btn small ghost" onClick={() => edit(x)}>Edit</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}
