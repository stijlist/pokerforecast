json.array!(@poker_nights) do |poker_night|
  json.extract! poker_night, :id, :date
  json.url poker_night_url(poker_night, format: :json)
end
