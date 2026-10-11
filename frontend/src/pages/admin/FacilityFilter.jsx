export default function FacilityFilter({ facilities, value, onChange }) {
  if (facilities.length < 2) return null;
  return (
    <select className="filter-select" value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">All my facilities</option>
      {facilities.map((f) => <option key={f.id} value={f.id}>{f.name}</option>)}
    </select>
  );
}
