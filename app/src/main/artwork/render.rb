#!/bin/env ruby

resolutions = {
	'mdpi' => 1,
	'hdpi' => 1.5,
	'xhdpi' => 2,
	'xxhdpi' => 3,
	'xxxhdpi' => 4,
}

def execute_cmd(cmd)
	puts cmd
	system cmd
end

images = {
    "cover_304_192.png" => ["cover.png", 304]
}

images.each do |source_filename, settings|

    output_filename, base_size = settings

    resolutions.each do |resolution, factor|
        path = "../res/drawable-#{resolution}/#{output_filename}"
        width = factor * base_size
        if source_filename.end_with? ".svg"
            execute_cmd "inkscape -f #{source_filename} -z -C -w #{width} -h #{width} -e #{path}"
        else
            execute_cmd "convert #{source_filename} -resize #{width} #{path}"
        end
    end
end
