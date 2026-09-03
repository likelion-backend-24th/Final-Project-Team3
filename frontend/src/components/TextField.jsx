export default function TextField({ label, error, className = '', ...props }) {
  return (
    <label className={`block ${className}`}>
      {label && <span className="block mb-2 text-sm text-text">{label}</span>}
      <input
        className="w-full bg-bg border border-border rounded-lg px-4 py-3 text-sm text-text placeholder:text-text-faint focus:outline-none focus:border-primary"
        {...props}
      />
      {error && <span className="block mt-1.5 text-xs text-danger">{error}</span>}
    </label>
  )
}
