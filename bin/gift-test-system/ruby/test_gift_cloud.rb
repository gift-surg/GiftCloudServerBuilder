require 'rspec'

require_relative 'client'

RSpec.describe GiftCloud::Client do
  # AUTHENTICATION ===================================
  it 'signs in' do
    skip 'not implemented'
  end
  
  it 'signs out' do
    skip 'not implemented'
  end
  # ==================================================
  
  # PROJECT ==========================================
  it 'creates a new project' do
    skip 'not implemented'
  end
  
  it 'does not re-create an existing project' do
    skip 'not implemented'
  end
  
  it 'lists all projects accessible to user' do
    skip 'not implemented'
  end
  
  it 'does not list any project not accessible to user' do
    skip 'not implemented'
  end
  # ==================================================
  
  # SUBJECT ==========================================
  it 'creates a new subject' do
    skip 'not implemented'
  end
  
  it 'does not re-create an existing subject' do
    skip 'not implemented'
  end
  
  it 'lists all subjects in a project' do
    skip 'not implemented'
  end
  # ==================================================
  
  # UPLOAD ===========================================
  it 'uploads zipped DICOM studies of a subject to a new session' do
    skip 'not implemented'
  end
  
  it 'uploads zipped DICOM studies of a subject to an existing session' do
    skip 'not implemented'
  end
  # ==================================================
  
  # PSEUDONYM ========================================
  it 'creates a new pseudonym for a subject' do
    skip 'not implemented'
  end
  
  it 'retrieves subject corresponding to an existing pseudonym' do
    skip 'not implemented'
  end
  
  it 'does not re-create an existing pseudonym' do
    skip 'not implemented'
  end
  # ==================================================
end