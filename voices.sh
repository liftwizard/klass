declare -a voices=(
	"Serena (Premium)"
	"Zoe (Premium)"
	"Stephanie (Enhanced)"
	"Daniel (Enhanced)"
	"Evan (Enhanced)"
	"Rishi"
	"Nathan (Enhanced)"
	"Oliver (Enhanced)"

	"Trinoids"
	"Zarvox"
)

for voice in "${voices[@]}"
do
	echo "Voice $voice"
	say --progress --voice "$voice" "[[volm 0.9]] This is an example of using text to speech with the voice called $voice."
done

