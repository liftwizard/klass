// Formatting config for `vp fmt` (oxfmt). Plain object export so no
// node_modules are needed; vite-plus is provided globally via mise.
export default {
	fmt: {
		useTabs: true,
		tabWidth: 4,
		printWidth: 120,
		proseWrap: "never",
		embeddedLanguageFormatting: "off",
		semi: true,
		singleQuote: false,
		bracketSpacing: false,
		trailingComma: "all",
		arrowParens: "always",
		overrides: [
			{
				files: [".yamllint.yaml", "**/*.yaml", "**/*.yml"],
				options: {
					useTabs: false,
					tabWidth: 2,
				},
			},
			{
				files: ["**/*.md"],
				options: {
					printWidth: 320,
				},
			},
		],
	},
};
