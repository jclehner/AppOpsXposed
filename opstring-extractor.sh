#!/bin/bash

BRANCH="cm-13.0"
RES="https://github.com/CyanogenMod/android_packages_apps_Settings/raw/$BRANCH/res/"
TMP=$(mktemp -t aoxXXXXXX)

PATTERNS=(
	's/_modify_/_write_/g'
	's/_wake_up/_alarm_wakeup/g'
	's/_mute_unmute/_mute/g'
	's/_sms_db/_sms/g'
	's/_keep_awake/_wake_lock/g'
	's/_auto_start/_boot_completed/g'
	's/_audio_focus/_take_audio_focus/g'
	's/_notification"/_post_notification"/g'
	's/_notification_toast/_post_notification/g'
	's/_draw_on_top/_system_alert_window/g'
	's/_media_buttons/_take_media_buttons/g'
	's/_use_body_sensors/_body_sensors/g'
	's/_data_change/_data_connect_change/g'
	's/_cell_scan/_neighboring_cells/g'
	#'s/_make_call/_call_phone/g'
	's/_superuser/_su/g'
	's/_start_at_boot/_boot_completed/g'
	's/_mobile_data/_data_connect/g'
	's/_toggle_(.*?)"/_$1_change"/g'
	's/_(labels|summaries)_(.*?)_volume/_$1_audio_$2_volume/g'
)

readarray() {
	builtin readarray -t "$1" &> /dev/null || read -r -a "$1"
}

grep() {
	if command -v ggrep &> /dev/null; then
		ggrep "$@"
	else
		command grep "$@"
	fi
}

copy() {
	[[ -f "$1" ]] && cp "$1" "$2"
}

download() {
	if [[ ! -z $OPSTRING_DOWNLOADS ]]; then

		if copy "$OPSTRING_DOWNLOADS/$1" "$2"; then
			echo -n "*"
		else
			# Maybe we have a matching file in another
			# directory of the same language?

			dir=$(dirname "$1")
			lang=${dir:7:2}
			base=$(basename "$1")

			if [[ ${dir:10:1} == "r" ]]; then
				# We have a regional locale; try the main language
				if copy "$OPSTRING_DOWNLOADS/values-$lang/$base" "$2"; then
					echo -n "($lang)*"
					return 0
				fi
			else
				# We have a language without a region specified;
				# try the regional locales (if any).
				files=( "$OPSTRING_DOWNLOADS/values-${lang}"-r??/$base "(skip)")
				valdir=$(basename "$(dirname "${files[0]}")")
				count=${#files[@]}

				if [[ $count -gt 0 && ${valdir:11} != "??" ]]; then
					if [[ $count -gt 1 ]]; then
						echo
						echo "$lang: select a $base file"
						select f in "${files[@]}"; do
							[[ $f == "(skip)" ]] && echo -n "$lang: DL!" && return 0

							locale=$(basename "$f")
							locale=${locale:7}
							if copy "$f" "$2"; then
								echo -n "$lang: DL*"
								return 0
							else
								echo "$lang: copy failed ($f/$base -> $2)"
							fi
						done
					else
						if copy "${files[0]}" "$2"; then
							echo -n "(${valdir:7})*"
						fi
					fi
				fi
			fi

			echo -n "!"
			return 1
		fi

		return 0
	fi

	if wget -O "$2" -q "$RES/$1"; then
		echo -n "*"
	else
		rm -f "$2"
		echo -n "!"
		return 1
	fi
}

extract_lang() {
	rm -f "$TMP".*

	echo -n "$1: "

	echo -n "DL"
	download "values-$1/strings.xml" "$TMP.dl2" 
	if ! download "values-$1/cm_strings.xml" "$TMP.dl1"; then
		use_arrays=1
	else
		if ! grep -qP 'app_ops_(summaries|labels)_' "$TMP.dl1" &> /dev/null; then
			use_arrays=1
		else
			use_arrays=
		fi
	fi

	if [[ -z $use_arrays ]]; then
		echo -n " "
		grep -P "app_ops_(summaries|labels)_" "$TMP.dl1" > "$TMP.ops"

		echo -n "GREP* SED"

		for p in ${PATTERNS[@]}; do
			perl -p -i -e "$p" "$TMP.ops"
		done

		echo -n "* "

		echo "  <!-- Categories -->" >> "$TMP.ops"
		grep "app_ops_categories_" "$TMP.dl1" >> "$TMP.ops"
	else
		if download "values-$1/arrays.xml" "$TMP.arr"; then

			echo -n " SYNTH"

			awk "/app_ops_summaries/,/\/string-array/" < "$TMP.arr" | grep -v string-array > "$TMP.sum"
			awk '/app_ops_labels/,/\/string-array/' < "$TMP.arr" | grep -v string-array > "$TMP.lab"
			awk '/app_ops_categories/,/\/string-array/' < "$TMP.arr" | grep -v string-array > "$TMP.cat"

			perl -p -i -e 's/<.+?>//g' "$TMP".{sum,lab,cat}
			perl -p -i -e 's/^\s+//g' "$TMP".{sum,lab,cat}

			readarray summaries < "$TMP.sum"
			readarray labels < "$TMP.lab"
			readarray categories < "$TMP.cat"

			size=${#summaries[@]}

			# The old app_ops_{summaries_labels} arrays contain 47 entries; the last two are
			# for OP_PROJECT_MEDIA and OP_ACTIVATE_VPN. The one before that should have been
			# OP_TOAST_WINDOW, but it's missing. This means that the last two array indices
			# must be shifted to be correct.

			if [[ $size -ne 47 || $size -ne ${#labels[@]} ]]; then
				echo "($size/${#labels[@]})!"
				return 1
			fi

			ops=$(grep -oP 'app_ops_summaries_(\w+)' res/values/ops.xml | sed -e 's/app_ops_summaries_//')
			i=0

			for op in $ops; do
				if [[ $op == "toast_window" ]]; then
					let i=$i-1
					s=
					l=
				elif [[ $i -lt $size ]]; then
					s=${summaries[$i]}
					l=${labels[$i]}
				else
					s=
					l=
				fi

				if [[ ! -z "$s" ]]; then
					echo "  <string name=\"app_ops_summaries_$op\">$s</string>" >> "$TMP.ops"
				else
					echo "  <!-- app_ops_summaries_$op missing -->" >> "$TMP.ops"
				fi

				if [[ ! -z "$l" ]]; then
					echo "  <string name=\"app_ops_labels_$op\">$l</string>" >> "$TMP.ops"
				else
					echo "  <!-- app_ops_labels_$op missing -->" >> "$TMP.ops"
				fi

				let i=$i+1
			done

			echo -n "* "
		else
			echo -n " "
		fi

		echo "  <!-- Categories -->" >> "$TMP.ops"

		cats=$(grep -oP 'app_ops_categories_(\w+)' res/values/ops.xml)
		i=0

		for c in $cats; do
			s=${categories[$i]}

			if [[ ! -z "$s" ]]; then
				echo "  <string name=\"$c\">$s</string>" >> "$TMP.ops"
			else
				echo "  <!-- $c missing -->" >> "$TMP.ops"
			fi

			let i=$i+1
		done
	fi


	if [[ ! -f "$TMP.dl2" ]]; then
		echo
		return 1
	fi

	echo -n "FINISH"

	echo "  <!-- Misc -->" >> "$TMP.ops"
	grep -P '"version_text"|"app_ops_' "$TMP.dl2" >> "$TMP.ops"

	perl -p -i -e 's/msgid=".*?">/>/g' "$TMP.ops"
	perl -p -i -e 's/<\/?xliff:.+?>//g' "$TMP.ops"

	out="res/values-$1/extracted.xml"

	echo > $out '<?xml version="1.0" encoding="utf-8"?>'
	echo >> $out "<!-- Auto-generated by opstring-extractor on $(date +%Y-%m-%d) from branch $BRANCH -->"

	cat >> $out <<EOF 
<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="ExtraTranslation">
  <!-- Labels and summaries -->
EOF
	cat >> $out "$TMP.ops"

	perl -p -i -e "s/^\s+/  /g" "$out"

	echo "</resources>" >> $out

	echo "*"
}

cd "$(dirname "$0")"

if [[ "$1" == "download" ]]; then
	if [[ -z "$2" ]]; then
		echo >&2 "usage: $0 download [target folder]"
		exit 1
	fi

	mkdir -p "$2" || exit 1
	unset OPSTRING_DOWNLOADS
fi

if [[ $# -eq 0 || "$1" == "download" ]]; then
	for dir in res/values-??{,-r??}; do
		lang=$(basename "$dir")
		lang=${lang:7}

		if [[ -z "$1" ]]; then
			if [[ "${lang:3:1}" == "r" && -e "res/values-${lang:0:2}/extracted.xml" ]]; then
				echo "$lang: SKIPPED"
			else
				extract_lang "$lang"
			fi
		else
			base=$(basename "$dir")
			d="$2/$base"
			mkdir -p "$d" || exit 1
			cd "$d"

			echo -n "$lang: DL"
			download "$base/strings.xml" "strings.xml"
			download "$base/cm_strings.xml" "cm_strings.xml"
			download "$base/arrays.xml" "arrays.xml"
			echo

			cd - &> /dev/null
		fi
	done
else
	extract_lang "$1"
fi

rm -f "$TMP $TMP.*"
