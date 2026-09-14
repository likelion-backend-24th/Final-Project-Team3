export default function SelectField({ label, options, placeholder, error, className = '', ...props }) {
  return (
    <label className={`block ${className}`}>
      {label && <span className="block mb-2 text-sm text-text">{label}</span>}
      <select
        className="w-full bg-bg border border-border rounded-lg px-4 py-3 text-sm text-text focus:outline-none focus:border-primary"
        {...props}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {error && <span className="block mt-1.5 text-xs text-danger">{error}</span>}
    </label>
  )
}
