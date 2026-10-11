import { useCallback, useEffect, useState } from "react";
import { api } from "../../api";
import { Badge, Err, money } from "../../ui";

export default function MarketTab() {
  const [listings, setListings] = useState([]);
  const [err, setErr] = useState("");

  const load = useCallback(() => {
    api("/api/super/listings").then(setListings).catch((e) => setErr(e.message));
  }, []);
  useEffect(() => { load(); }, [load]);

  const remove = async (id) => {
    setErr("");
    try { await api(`/api/listings/${id}`, { method: "DELETE" }); load(); } catch (e) { setErr(e.message); }
  };

  return (
    <>
      <Err msg={err} />
      <table className="card">
        <thead><tr><th>Title</th><th>Seller</th><th>Price</th><th>Status</th><th></th></tr></thead>
        <tbody>
          {listings.map((l) => (
            <tr key={l.id}>
              <td>{l.title}</td><td>{l.seller.fullName}</td><td>{money(l.price)}</td><td><Badge s={l.status} /></td>
              <td><button className="btn small ghost" onClick={() => remove(l.id)}>Remove</button></td>
            </tr>
          ))}
          {!listings.length && <tr><td colSpan="5" className="muted">No listings.</td></tr>}
        </tbody>
      </table>
    </>
  );
}
