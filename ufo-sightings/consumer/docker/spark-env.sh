while IFS='=' read -r name value ; do
    [ "${name:-}" ] && export "${name}"="${value}"
done < <(env)