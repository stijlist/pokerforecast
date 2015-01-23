class CreatePokerNights < ActiveRecord::Migration
  def change
    create_table :poker_nights do |t|
      t.datetime :date

      t.timestamps
    end
  end
end
