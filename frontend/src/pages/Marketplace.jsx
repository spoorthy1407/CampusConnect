import { useCallback, useEffect, useState } from "react";
import { api } from "../api";
import { Err, money, imgUrl } from "../ui";

const CATS = ["Books", "Electronics", "Furniture", "Clothing", "Stationery", "Sports", "Other"];

export default function Marketplace() {
  const [items, setItems] = useState([]);
  const [filters, setFilters] = useState({ q: "", category: "", maxPrice: "" });
  const [show, setShow] = useState(false);
  const [form, setForm] = useState({ title: "", category: "Books", price: "" });
  const [file, setFile] = useState(null);
  const [err, setErr] = useState("");

  const load = useCallback(() => {
    const p = new URLSearchParams();
    Object.entries(filters).forEach(([k, v]) => v && p.set(k, v));
    api(`/api/listings?${p}`).then(setItems).catch((e) => setErr(e.message));
  }, [filters]);

  useEffect(() => { load(); }, [load]);

  const post = async (e) => {
    e.preventDefault();
    setErr("");
    const fd = new FormData();
    Object.entries(form).forEach(([k, v]) => fd.append(k, v));
    if (file) fd.append("image", file);
    try {
      await api("/api/listings", { method: "POST", form: fd });
      setForm({ title: "", category: "Books", price: "" }); setFile(null); setShow(false); load();
    } catch (ex) { setErr(ex.message); }
  };

  return (
    <>
      <div className="row">
        <h2>Marketplace</h2>
        <button className="btn" onClick={() => setShow(!show)}>{show ? "Close" : "+ Sell an item"}</button>
      </div>
      <Err msg={err} />

      {show && (
        <form className="card form" onSubmit={post}>
          <input placeholder="Title" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
          <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
            {CATS.map((c) => <option key={c}>{c}</option>)}
          </select>
          <input type="number" min="0" placeholder="Price (₹)" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required />
          <input type="file" accept="image/*" onChange={(e) => setFile(e.target.files[0])} />
          <button className="btn">Post listing</button>
        </form>
      )}

      <div className="filters card">
        <input placeholder="Search title…" value={filters.q} onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
        <select value={filters.category} onChange={(e) => setFilters({ ...filters, category: e.target.value })}>
          <option value="">All categories</option>
          {CATS.map((c) => <option key={c}>{c}</option>)}
        </select>
        <input type="number" min="0" placeholder="Max price" value={filters.maxPrice} onChange={(e) => setFilters({ ...filters, maxPrice: e.target.value })} />
      </div>

      <div className="grid">
        {items.map((l) => (
          <div className="card" key={l.id}>
            {l.imageUrl ? <img className="thumb" src={imgUrl(l.imageUrl)} alt={l.title} /> : <div className="thumb ph">No photo</div>}
            <h3>{l.title}</h3>
            <p className="muted">{l.category} · {l.seller.fullName}</p>
            <div className="row"><b>{money(l.price)}</b>
              <a className="btn small" href={`mailto:${l.seller.email}?subject=${encodeURIComponent("Interested in: " + l.title)}`}>Contact seller</a>
            </div>
          </div>
        ))}
        {!items.length && <p className="muted">No listings match.</p>}
      </div>
    </>
  );
}
