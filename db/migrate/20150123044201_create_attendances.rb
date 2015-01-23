class CreateAttendances < ActiveRecord::Migration
  def change
    create_table :attendances do |t|
      t.integer :player_id
      t.integer :poker_night_id

      t.timestamps
    end
  end
end
