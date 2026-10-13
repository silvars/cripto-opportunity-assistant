import js from "@eslint/js";
import tseslint from "typescript-eslint";
import reactHooks from "eslint-plugin-react-hooks";

// eslint-config-next@16.4.0 quebra com referência circular ao carregar via FlatCompat
// (bug na composição legada do plugin react). Usamos flat config nativo em vez do bridge.
export default tseslint.config(
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    plugins: { "react-hooks": reactHooks },
    rules: {
      ...reactHooks.configs.recommended.rules,
    },
  },
  {
    ignores: [".next/**", "node_modules/**"],
  },
);
